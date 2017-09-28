package Models

import play.api.libs.json.{Json, OFormat, OWrites, Reads}

case class Application(name:String, version:String, image:String)
object Application{

  val defaultApplications = List(
    Application("Elasticsearch", "latest", "manatee/docker/elastisearch_nxp:latest"),
    Application("Kibana", "latest", "manatee/docker/kibana_nxp:latest"),
    Application("Nginx", "latest", "nginx:latest"),
  )

  implicit val applicationFormat: OFormat[Application] = Json.format[Application]
  implicit val applicationWrites: OWrites[Application] = Json.writes[Application]
  implicit val applicationReads: Reads[Application] = Json.reads[Application]
  def getDefaultApplications(): List[Application] = defaultApplications.sortBy(_.name)
}
