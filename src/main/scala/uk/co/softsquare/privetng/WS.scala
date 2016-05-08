package uk.co.softsquare.privetng

import java.util.concurrent.Executors

import play.api.libs.json.{JsValue, Reads}
import uk.co.softsquare.privetng.auth.ApplicationInfo

import scala.concurrent.{ExecutionContext, Future}

trait Http {
  def post[T](url: String, body: Map[String, Seq[String]])(implicit fjs: Reads[T]): Future[T]
  def postJson[T](url: String, body: JsValue, token: String)(implicit fjs: Reads[T]): Future[T]
  def postJsonOpt[T](url: String, body: JsValue, token: String)(implicit fjs: Reads[T]): Future[ResponseOpt[T]]
  def shutdown()
}

trait HttpComponent {
  def executionContext: ExecutionContext
  def applicationInfo: ApplicationInfo
  def http: Http
}

/**
 * Play WS shim to use outside playframework
 */
object WS {
  val builder = new com.ning.http.client.AsyncHttpClientConfig.Builder()
  val client = new play.api.libs.ws.ning.NingWSClient(builder.build())

  def url(url: String) = client.url(url)
  def urlWithHeaders(app: ApplicationInfo, endpoint: String, token: String) = url(endpoint).withHeaders(
    "X-Application" -> app.applicationKey,
    "X-Authentication" -> token,
    "content-type" -> "application/json"
  )
}

trait WSHttpComponent extends HttpComponent {
  override def http: Http = new Http {

    implicit val ec = executionContext

    override def post[T](url: String, body: Map[String, Seq[String]])(implicit fjs: Reads[T]): Future[T] =
      WS.url(url)
        .withHeaders(
          "X-Application" -> applicationInfo.applicationKey,
          "Accept" -> "application/json"
        )
        .withRequestTimeout(1000)
        .withFollowRedirects(follow = true)
        .post(body).map(_.json.as[T])

    override def postJson[T](url: String, body: JsValue, token: String)(implicit fjs: Reads[T]): Future[T] =
      WS.urlWithHeaders(applicationInfo, url, token).post(body).map(_.json.as[T])

    override def postJsonOpt[T](url: String, body: JsValue, token: String)(implicit fjs: Reads[T]): Future[ResponseOpt[T]] =
      WS.urlWithHeaders(applicationInfo, url, token).post(body).map(response => ResponseOpt(response.status, response.json.asOpt[T]))

    /**
     * Can't be run in the same thread pool as the WS.client
     */
    override def shutdown(): Unit = new Thread(new Runnable {
      override def run(): Unit = WS.client.close()
    }).start()

  }
}

case class ResponseOpt[T](status: Int, response: Option[T]) {
  def isOK() = status == 200
}
