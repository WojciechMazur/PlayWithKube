package services.kube

import java.util.Calendar

import Models.Application
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

  case class DeploymentFinder(name:String, namespace:String)
  implicit val deploymentFinderReads: OFormat[DeploymentFinder] = Json.format[DeploymentFinder]

  case class ResourcesRequirements(limits: Map[String, String], requests: Map[String, String])
  implicit val resourcesRequirementsReads: OFormat[ResourcesRequirements] = Json.format[ResourcesRequirements]

  case class Deployment(name:String,
                        namespace: String,
                        version:String,
                        replicas:Int,
                        resources: ResourcesRequirements,
                        applicationConfig:Map[String, String])
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

  def deleteDeployment(name:String, namespace:String): Unit ={
    if(namespace=="kube-system")
      throw new IllegalArgumentException("Cannot delete kube-system deployment")
    try{
    kubeInstance.extensions().deployments().inNamespace(namespace).withName(name).delete()
    }catch {
      case ex:Exception => println(ex.getMessage)
        throw ex
    }
  }

  def createDeployment(d: Deployment): Unit ={
    d match {
      case Deployment("elasticsearch", _, _, _,_,_) =>
        println(s"${Calendar.getInstance().getTime.toString} :: Creating elasticsearch deployment\n${Json.toJson(d)}")
        createElasticsearchDeploymentHelper(deployment = d)
      case Deployment("kibana",_,_,_,_,_)=>
        println(s"${Calendar.getInstance().getTime.toString} :: Creating kibana deployment\n${Json.toJson(d)}")
        createKibanaDeploymentHelper(deployment=d)
      case Deployment("nginx",_,_,_,_,_) =>
        println(s"${Calendar.getInstance().getTime.toString} :: Creating kibana deployment\n${Json.toJson(d)}")
        createNginxDeploymentHelper(d);
      case _ => println(s"Unknown deployment ${d.name} with version ${d.version}")
    }
  }

  private def createElasticsearchDeploymentHelper(deployment: Deployment): Unit ={
    if(deployment.applicationConfig.getOrElse("productionMode", false)=="true"){
      println("Production mode ON")
      for(nodeType <- List("Master","Data","Client")){
        if(deployment.applicationConfig.getOrElse("is"+nodeType, false)=="true") {
          println(s"Creating elasticsearch ${nodeType.toLowerCase} node")
          createElasticsearchDeployment(
            s"${deployment.name}-${nodeType.toLowerCase()}",
            deployment.namespace,
            parseResourcesMap(deployment.resources.limits),
            parseResourcesMap(deployment.resources.requests),
            version = deployment.version,
            expose = deployment.applicationConfig.getOrElse("expose", "true") =="true",
            desiredReplicas = deployment.applicationConfig.getOrElse(nodeType.toLowerCase + "Nodes", 1).toString.toInt,
            master = nodeType.toLowerCase == "master",
            data = nodeType.toLowerCase == "data",
            http = nodeType.toLowerCase == "client")
        }
      }
    }else{
      println("Production mode OFF")
      createElasticsearchDeployment(
        deployment.name,
        deployment.namespace,
        parseResourcesMap(deployment.resources.limits),
        parseResourcesMap(deployment.resources.requests),
        expose = deployment.applicationConfig.getOrElse("expose", true)=="true",
        desiredReplicas = deployment.replicas,
        version = deployment.version
      )
    }
  }

  private def createKibanaDeploymentHelper(deployment: Deployment): Unit = {
    createKibanaDeployment(
      deployment.name,
      deployment.namespace,
      parseResourcesMap(deployment.resources.limits),
      parseResourcesMap(deployment.resources.requests),
      expose = deployment.applicationConfig.getOrElse("expose", "true") == "true",
      desiredReplicas = deployment.replicas,
      version = deployment.version)
  }

  private def createNginxDeploymentHelper(deployment: Deployment): Unit ={
    createNginxDeployment(
      deployment.name,
      deployment.namespace,
      parseResourcesMap(deployment.resources.limits),
      parseResourcesMap(deployment.resources.requests),
      expose = deployment.applicationConfig.getOrElse("expose", "true") == "true",
      desiredReplicas = deployment.replicas,
      version = deployment.version)
  }

  private def exposeKibanaDeployment(namespace: String): Unit = {
    Future {
         Services.createService(
           name = "kibana",
           port = 5601,
           serviceType = LoadBalancer.toString,
           selector = "app=kibana",
           externalIP = "10.132.15.190")
       }
  }

  private def createKibanaDeployment(name:String, namespace:String, limits:Map[String, Quantity], requests:Map[String, Quantity],
                             expose:Boolean=true, desiredReplicas:Int=2, version:String): Unit ={

     val elasticLabels = Map("app"->name.split("-").head)

    val resources = new ResourceRequirementsBuilder()
      .withLimits(limits.asJava)
      .withRequests(requests.asJava)
      .build()

    val image =  Application.getDefaultApplications().filter(_.name.toLowerCase == name).find(_.version.toLowerCase==version).orNull.image
    if (image==null)
       throw new IllegalArgumentException(s"Image $image not found")
    val containers = new ContainerBuilder()
      .withName("kibana")
      .withImage(image)
      .withImagePullPolicy("IfNotPresent")
      .addNewPort().withName("http").withContainerPort(5601).endPort()
      .withCommand("bin/kibana")
      .withArgs( "-e http://elasticsearch:9200",
                 "-H 0.0.0.0")
      .addNewEnv().withName("KIBANA_ES_URL").withValue("http://elasticsearch:9200").endEnv()
      .addNewEnv().withName("CLUSTER_NAME").withNewValueFrom()
        .withNewFieldRef().withFieldPath("metadata.namespace").endFieldRef()
      .endValueFrom().endEnv()
      .withResources(resources)
      .build()

    val metadata = new ObjectMetaBuilder()
      .withName(name)
      .withNamespace(namespace)
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
          .withLabels(elasticLabels.asJava)
        .endMetadata()
      .endTemplate()
      .endSpec()
      .build()

     val deploymentStatus = Future{kubeInstance.extensions().deployments().create(deploymentInstance)}
    if (expose)
        exposeKibanaDeployment(namespace)

    Await.result[io.fabric8.kubernetes.api.model.extensions.Deployment](deploymentStatus, 10 seconds) match {
      case _: io.fabric8.kubernetes.api.model.extensions.Deployment => new StatusBuilder().withCode(201).build()
    }
  }

  private def createNginxDeployment(name: String, namespace: String, limits: Map[String, Quantity], requests: Map[String, Quantity], expose: Boolean=true, desiredReplicas:Int=1, version: String): Unit = {
    val elasticLabels = Map("app"->name.split("-").head)

    val resources = new ResourceRequirementsBuilder()
      .withLimits(limits.asJava)
      .withRequests(requests.asJava)
      .build()

    val image =  Application.getDefaultApplications().filter(_.name.toLowerCase == name).find(_.version.toLowerCase==version).orNull.image
    if (image==null)
       throw new IllegalArgumentException(s"Image $image not found")

    val containers = new ContainerBuilder()
      .withName("nginx")
      .withImage(image)
      .withImagePullPolicy("IfNotPresent")
      .addNewPort().withName("http").withContainerPort(80).endPort()
      .withResources(resources)
      .build()

    val metadata = new ObjectMetaBuilder()
      .withName(name)
      .withNamespace(namespace)
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
          .withLabels(elasticLabels.asJava)
        .endMetadata()
      .endTemplate()
      .endSpec()
      .build()

     val deploymentStatus = Future{kubeInstance.extensions().deployments().create(deploymentInstance)}
    if (expose)
        Future {
         Services.createService(
           name = "nginx",
           port = 80,
           serviceType = LoadBalancer.toString,
           selector = "app=nginx",
           externalIP = "10.132.15.190")
       }

    Await.result[io.fabric8.kubernetes.api.model.extensions.Deployment](deploymentStatus, 10 seconds) match {
      case _: io.fabric8.kubernetes.api.model.extensions.Deployment => new StatusBuilder().withCode(201).build()
    }
  }

  private def createElasticsearchDeployment(name:String, namespace:String, limits:Map[String, Quantity], requests:Map[String, Quantity], master:Boolean=true, data:Boolean=true, http:Boolean=true,
                                    expose:Boolean=true, desiredReplicas:Int = 1, masterNodes:Int=0, version:String)
                                    : Status ={

    val masterNodesQuantity = if(masterNodes!=0) masterNodes else desiredReplicas/2+1


    val elasticLabels = Map("app"->name.split("-").head, "master"-> master.toString, "data" -> data.toString, "http" -> http.toString)

    val resources = new ResourceRequirementsBuilder()
      .withLimits(limits.asJava)
      .withRequests(requests.asJava)
      .build()

    val initContainerAnnotation = Map(
      "pod.beta.kubernetes.io/init-containers" -> buildInitContainerAnnotation(name="sysctl-busybox", image="busybox",
        command = "sysctl -w vm.max_map_count=262144")
    )

    val image =  Application.getDefaultApplications()
      .filter(_.name.toLowerCase == name.split("-").head)
      .find(_.version.toLowerCase==version)
      .orNull.image
    if (image==null)
       throw new IllegalArgumentException(s"Image $image not found")
    val containers = new ContainerBuilder()
      .withName("elasticsearch")
      .withImage(image)
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
      .withName(name)
      .withNamespace(namespace)
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
        exposeElasticsearchDeployment(master, http, namespace)

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
         selector = "app=elasticsearch,http=true",
         externalIP = "10.132.15.190")
     }

   }

  private def parseResourcesMap(resources: Map[String, String]): Map[String, io.fabric8.kubernetes.api.model.Quantity] = {
    for (res <- resources)
      yield res._1 -> new QuantityBuilder().withAmount(res._2).build()
  }
}
