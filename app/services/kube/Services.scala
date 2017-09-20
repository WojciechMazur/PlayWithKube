package services.kube

import io.fabric8.kubernetes.api.model._
import services.kube.Instances.kubeInstance

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

object Services {

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

  def createService(name: String, namespace: String = "default", port: Int, serviceType: String, selector: String) {
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