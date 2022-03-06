import zhttp.http._
import zhttp.service.Server
import zio._

object ZIOHTTPExample extends zio.App {

  val app: HttpApp[Any, Nothing] = Http.collect {
    case Method.GET -> !! / "text" => Response.text("Hello, World!")
  }
  
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    Server.start(8090, app).exitCode
}