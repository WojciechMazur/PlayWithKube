package services.kube

import io.fabric8.kubernetes.client.{DefaultKubernetesClient, KubernetesClient}

object Instances {
    val kubeInstance: KubernetesClient = initFabric8Client()


  private def initFabric8Client(hostURL: String="https://localhost:6443"): KubernetesClient ={
    new DefaultKubernetesClient()
  }

  override def finalize(): Unit = {
    super.finalize()
    kubeInstance.close()
  }
}
