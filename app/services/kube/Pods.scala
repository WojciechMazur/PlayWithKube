package services.kube

import java.util

import io.fabric8.kubernetes.api.model.{OwnerReference, Pod, PodList, PodListBuilder}
import io.fabric8.kubernetes.api.model.extensions.ReplicaSet
import services.kube.Instances.kubeInstance
import services.kube.ReplicaSets.listReplicaSets

import scala.collection.JavaConverters._



object Pods {
  def listPods(namespace:String="") : PodList = {

    try {
      val list = namespace match {
        case ns if ns == "" => kubeInstance.pods().inAnyNamespace().list()
        case `namespace` => kubeInstance.pods().inNamespace(namespace).list()
      }
      list
    }catch{
      case ex:Exception => System.err.println("Cannot get pods" + ex.getMessage)
        new PodList()
    }
  }

  def listPodsInDeployment(deployment:String, namespace:String): PodList = {
    val replicaSets = listReplicaSets(namespace)
    val ownerReferences = for(
      replica<-replicaSets.getItems.asScala;
      owners<-replica.getMetadata.getOwnerReferences.asScala
      if owners.getName==deployment)
      yield replica

    val selected = ownerReferences.toList.head
    selected match {
      case _: ReplicaSet => filterPodsByReplicaSet(listPods(namespace), selected)
      case null => new PodList()
    }
  }

  private def filterPodsByReplicaSet(podList: PodList, replicaSet: ReplicaSet):PodList={
    if(podList.getItems.size()>0){
      val uid = replicaSet.getMetadata.getUid
      val pods: List[Pod] = podList.getItems.asScala.filter{
        case pod: Pod
          if pod.getMetadata.getOwnerReferences.size()>0 &&
             pod.getMetadata.getOwnerReferences.get(0).getUid==uid => true
        case _ =>false
      }.toList
      new PodListBuilder().withItems(pods.asJava).build()
    }
    else new PodList()
  }

}

