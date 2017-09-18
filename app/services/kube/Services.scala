package services.kube

import io.fabric8.kubernetes.api.model.ServiceList
import Instances.kubeInstance

object Services {

  def listServices(namespaces: String=""): ServiceList = {
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
}
