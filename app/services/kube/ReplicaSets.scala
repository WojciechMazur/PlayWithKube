package services.kube

import io.fabric8.kubernetes.api.model.extensions.ReplicaSetList
import services.kube.Instances.kubeInstance

object ReplicaSets {
  def listReplicaSets(namespace:String=null):ReplicaSetList = {
    try {
      namespace match {
        case null => kubeInstance.extensions().replicaSets().inAnyNamespace().list()
        case `namespace` => kubeInstance.extensions().replicaSets().inNamespace(namespace).list()
      }
    }catch {
      case ex:Exception=> System.err.println("Cannot list replica sets " + ex.getMessage)
        new ReplicaSetList()
    }

  }
}
