package Models

class Application (name: String, version:String, image:String){
  def this(name:String, image:String){
    this(name, "latest", image)
  }

  override def toString: String = s"{name: $name, version: $version, image: $image}"
}
