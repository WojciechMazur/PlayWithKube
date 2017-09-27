package services.kube

import java.net.InetAddress

import io.fabric8.kubernetes.api.model._
import play.api.libs.json.{Json, OFormat}
import services.kube.Deployments.ResourcesRequirements
import services.kube.Instances.kubeInstance

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

object Services {
  case class ServiceFinder(name:String, namespace:String)
  implicit val serviceFinderformat: OFormat[ServiceFinder] = Json.format[ServiceFinder]

  object ServiceType extends Enumeration {
    type ServiceType = Value
    val ClusterIP: Services.ServiceType.Value = Value("ClusterIP")
    val NodePort: Services.ServiceType.Value = Value("NodePort")
    val LoadBalancer: Services.ServiceType.Value = Value("LoadBalancer")
  }

  def listServices(namespaces: String = ""): ServiceList = {
    try {
      val servicesList = namespaces match {
        case ns: String if ns != "" => kubeInstance.services().inNamespace(ns).list()
        case _ => kubeInstance.services().inAnyNamespace().list()
      }
      servicesList
    } catch {
      case ex: Exception =>
        System.err.println("Cannot get services" + ex.getMessage)
        new ServiceList()
    }
  }

  def deleteService(name:String, namespace:String): Unit ={
    println(s"Deleting service $name in $namespace namespace")
    if(namespace!="kube-system"){
      try{
        kubeInstance.services().inNamespace(namespace).withName(name).delete()
      }catch {
        case ex:Exception=> println(ex.getMessage)
          throw ex;
      }
    }
    else
      throw new IllegalArgumentException("Cannot delete system deployments")
  }

  def createService(name: String, namespace: String = "default", port: Int, serviceType: String, selector: String, externalIP:String=null) {
    val servicePort = new ServicePortBuilder()
      .withName(name)
      .withPort(port)
      .withNewTargetPort(port)
      .build()

    val parsedSelector = selector.split(",")
      .map(_.split("="))
      .map { case Array(key, value) => (key, value) }
      .toMap.asJava

    val serviceSpec = new ServiceSpecBuilder()
        .withType(serviceType)
        .withPorts(servicePort)
        .withSelector(parsedSelector)
      .build()

    if(serviceType!="ClusterIP")
      serviceSpec.setExternalIPs(List("10.132.15.190").asJava)

    println(serviceSpec.getExternalIPs)

    Future {
      kubeInstance.services().createNew()
        .withNewMetadata()
        .withName(name)
        .withNamespace(namespace)
        .endMetadata()
        .withSpec(serviceSpec)
        .done()
    } recover {
      case ex: Throwable if ex.getMessage.contains("409") => new StatusBuilder().withCode(304).withMessage(s"Service with name $name already exists in namespace $namespace").build()
      case ex: Throwable => new StatusBuilder().withCode(304).withMessage("Cannot create service. Reason:" + ex.getMessage).build()
    }
  }
}