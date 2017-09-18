package services.kube
import io.fabric8.kubernetes.api.model.Namespace
import services.kube.Instances.kubeInstance
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.collection.JavaConverters._

object Namespaces {

  def listNamespaces: List[Namespace] = {
    kubeInstance.namespaces().list().getItems.asScala.toList
  }
}