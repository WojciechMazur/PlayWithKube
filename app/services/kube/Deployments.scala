package services.kube

import io.fabric8.kubernetes.api.model.extensions.DeploymentList
import services.kube.Instances.kubeInstance

import scala.concurrent.Future
import scala.util.{Failure, Success}

object Deployments {

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

  def listAvailableImages():List[String]={
    List("Elasticsearch 5.5.1", "Kibana 5.5.1", "Nginx")
  }
}
