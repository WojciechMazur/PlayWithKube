package controllers

import javax.inject.Inject

import io.fabric8.kubernetes.api.model.PodList
import io.fabric8.kubernetes.api.model.extensions.DeploymentList
import play.api.mvc.{AbstractController, ControllerComponents}
import services.kube.Deployments.listDeployments
import services.kube.Pods
import services.kube.Services.listServices

import scala.concurrent.Await
import scala.language.postfixOps
import scala.concurrent.duration._

class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  def index = Action {
    Ok(views.html.index())
  }

  def namespaces = Action{
    Ok(views.html.namespaces(services.kube.Namespaces.listNamespaces))
  }

  def listDeploymentsInNamespace(namespace: String) = Action{
    val deployments: DeploymentList = listDeployments(namespace)
    Ok(views.html.deployments(deployments, namespace))
  }

  def listDeploymentsInPod(namespace:String, deploymentName:String) = Action{
    val pods = Pods.listPodsInDeployment(deploymentName, namespace)
    Ok(views.html.pods(pods, namespace, deploymentName))
  }

  def listAllDeployments = Action{
    val deployments = listDeployments()
    Ok(views.html.deployments(deployments))
  }


  def listAllServices = Action{
    val services = listServices()
    Ok(views.html.services(services))
  }

}

