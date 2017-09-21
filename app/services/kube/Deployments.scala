package services.kube

import io.fabric8.kubernetes.api.model.extensions.{DeploymentBuilder, DeploymentList}
import io.fabric8.kubernetes.api.model.{Status, _}
import play.api.libs.json._
import services.kube.Instances.kubeInstance
import services.kube.Services.ServiceType._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

object Deployments {
  case class Quantity(name:String, amount:String)
  implicit val quantityFormat: OFormat[Quantity] = Json.format[Quantity]

  case class ResourcesRequirements(limits: List[Quantity], requests: List[Quantity])
  implicit val resourcesRequirementsReads: OFormat[ResourcesRequirements] = Json.format[ResourcesRequirements]

  case class Deployment(name:String, namespace: String, version:String, resources: ResourcesRequirements)
  implicit val deploymentsReads: OFormat[Deployment] = Json.format[Deployment]



  def listDeployments(namespace: String = null): DeploymentList = {
    val deploymentsList: DeploymentList =
      namespace match {
        case null => kubeInstance.extensions().deployments()
          .inAnyNamespace()
          .list()
        case `namespace` => kubeInstance.extensions().deployments()
          .inNamespace(namespace)
          .list()
    }
    deploymentsList match {
      case list:DeploymentList => list
      case _ => throw new ClassNotFoundException
    }
  }

  def createDeployment(d: Deployment): Unit ={
    d match {
      case Deployment("elasticsearch", _, _, _) =>
        println("Creating elasticsearch deployment")
        createElasticsearchDeployment(deployment = d)
      case _ => println(s"Unknown deployment ${d.name} with version ${d.version}")
    }
  }

   def createElasticsearchDeployment(deployment: Deployment, master:Boolean=true, data:Boolean=true, http:Boolean=true,
                                    expose:Boolean=true, desiredReplicas:Int = 1, masterNodes:Int=0)
                                    : Status ={

    val masterNodesQuantity = if(masterNodes!=0) masterNodes else desiredReplicas/2+1



    val elasticLabels = Map("app"->deployment.name, "master"-> master.toString, "data" -> data.toString, "http" -> http.toString)

    val resources = new ResourceRequirementsBuilder()
      .withLimits(parseResourcesMap(deployment.resources.limits).asJava)
      .withRequests(parseResourcesMap(deployment.resources.requests).asJava)
      .build()

    val initContainerAnnotation = Map(
      "pod.beta.kubernetes.io/init-containers" -> buildInitContainerAnnotation(name="sysctl-busybox", image="busybox",
        command = "sysctl -w vm.max_map_count=262144")
    )

    val containers = new ContainerBuilder()
      .withName("elasticsearch")
      .withImage(s"manatee/docker/elastisearch_nxp:"+deployment.version)
      .withImagePullPolicy("IfNotPresent")
      .addNewPort().withName("http").withContainerPort(9200).endPort()
      .addNewPort().withName("transport").withContainerPort(9300).endPort()
      .withNewSecurityContext()
        .withNewCapabilities()
          .withAdd("IPC_LOCK", "SYS_RESOURCE")
        .endCapabilities()
        .withPrivileged(true)
      .endSecurityContext()
      .withCommand("bin/elasticsearch")
      .withArgs( "-Ediscovery.zen.ping.unicast.hosts=elasticsearch-discovery",
                s"-Ediscovery.zen.minimum_master_nodes=$masterNodesQuantity",
                 "-Enode.name=${NODE_NAME}",
                s"-Enode.master=$master",
                s"-Enode.data=$data",
                s"-Ehttp.enabled=$http",
                 "-Ecluster.name=${NAMESPACE}")
      .addNewEnv().withName("ES_JAVA_OPTS").withValue("-Xms256m -Xmx256m").endEnv()
      .addNewEnv().withName("NODE_NAME").withNewValueFrom()
        .withNewFieldRef().withFieldPath("metadata.name").endFieldRef()
      .endValueFrom().endEnv()
      .addNewEnv().withName("NAMESPACE").withNewValueFrom()
        .withNewFieldRef().withFieldPath("metadata.namespace").endFieldRef()
      .endValueFrom().endEnv()
      .withResources(resources)
      .build()

    val metadata = new ObjectMetaBuilder()
      .withName(deployment.name)
      .withNamespace(deployment.namespace)
      .withLabels(elasticLabels.asJava)
      .build()

    val deploymentInstance = new DeploymentBuilder()
      .withMetadata(metadata)
      .withNewSpec()
        .withReplicas(desiredReplicas)
        .withNewTemplate()
          .withNewSpec()
            .withContainers(containers)
        .endSpec()
        .withNewMetadata()
          .addToAnnotations(initContainerAnnotation.asJava)
          .withLabels(elasticLabels.asJava)
        .endMetadata()
      .endTemplate()
      .endSpec()
      .build()

     val deploymentStatus = Future{kubeInstance.extensions().deployments().create(deploymentInstance)}
    if (expose)
        exposeElasticsearchDeployment(master, http, deployment.namespace)

    Await.result[io.fabric8.kubernetes.api.model.extensions.Deployment](deploymentStatus, 10 seconds) match {
      case _: io.fabric8.kubernetes.api.model.extensions.Deployment => new StatusBuilder().withCode(201).build()
    }
   }

  private def buildInitContainerAnnotation(name:String, image:String, command:String, image_pull_policy:String="IfNotPresent",
                                           privileged:Boolean=true):String={

    val commands: String =
      (for(s <- command.split(" "))
        yield "\"".concat(s).concat("\""))
      .mkString(", ")

    new StringBuilder()
      .append("[")
        .append("{")
          .append(s"${'"'}name${'"'}: ${'"'}$name${'"'},")
          .append(s"${'"'}image${'"'}: ${'"'}$image${'"'},")
          .append(s"${'"'}command${'"'}: [$commands],")
          .append(s"${'"'}imagePullPolicy${'"'}:${'"'}$image_pull_policy${'"'},")
          .append(s"${'"'}securityContext${'"'}:{${'"'}privileged${'"'}:$privileged}")
        .append("}")
      .append("]")
      .mkString
  }
   private def exposeElasticsearchDeployment(master: Boolean, http: Boolean, namespace:String): Unit = {

     if (master) {
       Future {
         Services.createService(
           name = "elasticsearch-discovery",
           port = 9300,
           serviceType = ClusterIP.toString,
           selector = "app=elasticsearch,master=true")
       }
     }

     if (http) Future {
       Services.createService(
         name = "elasticsearch",
         port = 9200,
         serviceType = LoadBalancer.toString,
         selector = "app=elasticsearch,http=true")
     }

   }

  private def parseResourcesMap(resources: List[Quantity]): Map[String, io.fabric8.kubernetes.api.model.Quantity] = {
    (for (res <- resources)
      yield res.name -> new QuantityBuilder().withAmount(res.amount).build()).toMap
  }


}
