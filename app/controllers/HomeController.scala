package controllers


import io.fabric8.kubernetes.api.model.extensions.DeploymentList
import play.api.libs.json.{JsValue, Json}
import play.api.routing._
import services.kube.Deployments.{Deployment, listDeployments}
import services.kube.{Deployments, Pods}
import services.kube.Services.listServices
import javax.inject.Inject

import Models.Application
import play.api.mvc._

import scala.language.postfixOps

class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  def jsRoutes = Action { implicit request =>
    println("jsRoutes were used with request:" + request.body)
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        routes.javascript.HomeController.createDeployment,
        routes.javascript.HomeController.getImages,
        routes.javascript.HomeController.test
      )
    ).as("text/javascript")
  }

  def index = Action {
    Ok(views.html.index())
  }

  def namespaces = Action {
    Ok(views.html.namespaces(services.kube.Namespaces.listNamespaces))
  }

  def listDeploymentsInNamespace(namespace: String) = Action {
    val deployments: DeploymentList = listDeployments(namespace)
    Ok(views.html.deployments(deployments, namespace))
  }

  def listDeploymentsInPod(namespace: String, deploymentName: String) = Action {
    val pods = Pods.listPodsInDeployment(deploymentName, namespace)
    Ok(views.html.pods(pods, namespace, deploymentName))
  }

  def listAllDeployments = Action {
    val deployments = listDeployments()
    Ok(views.html.deployments(deployments))
  }

  def createDeployment: Action[JsValue] = Action(parse.json) { request =>
    println("Request body:" + Json.prettyPrint(request.body))
    val listDeployments = (request.body \ "items").as[List[Deployment]]
    try {
      if (listDeployments != null) {
        println("Gathered list of deployments" + listDeployments.toString())
        for (deployment <- listDeployments)
          Deployments.createDeployment(deployment)
      }
    }catch {
      case ex:Exception =>
        println(s"Bad request ${ex.getMessage}")
        BadRequest(ex.getMessage)
    }
    Ok("Ok")
  }

  def getImages = Action{
    val result = Json.obj("items" -> Application.getDefaultApplications())
    println("Sending list of available applications: \n" + Json.prettyPrint(result))
    Ok(result)
  }

  def listAllServices = Action {
    val services = listServices()
    Ok(views.html.services(services))
  }

  def test = Action(parse.json) { implicit request =>
    println(request.body.toString)
    Ok("Body:" )
  }

}

