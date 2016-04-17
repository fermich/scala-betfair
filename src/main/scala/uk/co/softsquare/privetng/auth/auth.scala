package uk.co.softsquare.privetng.auth

case class ApplicationInfo(applicationId: String, applicationKey: String, delayedApplicationKey: String)
object ApplicationInfo {
  def driverTestApp(): ApplicationInfo = ApplicationInfo(
    applicationId = "reactive-betfair",
    applicationKey = "HbdQdFs9jEWlWSGM",
    delayedApplicationKey = "f8A1qnUaxDLLDD1y")
}


case class Credentials(username: String, password: String) {
  override def toString: String = s"Credentials($username, ###)"
}
object Credentials {
  def fromConsole(): Credentials = Credentials(Console.readLine("username: "), Console.readLine("password: "))
}

object Session {
  import play.api.libs.json._
  implicit val loginReads = Json.reads[LoginResponse]
  case class LoginResponse(status: String, error: String, token: String, product: String)

  sealed trait Message
  case class Token(token: String) extends Message
  case object GetToken extends Message
}
