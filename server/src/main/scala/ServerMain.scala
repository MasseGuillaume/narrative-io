import zhttp.http._
import zhttp.service.Server
import zio._

object ServerMain extends zio.App {

  val app: UHttpApp = 
    Http.collect {
      case Method.GET -> Path() / "text" => Response.text("Hello, World!")
    }
  
  final def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    Server.start(8090, app).exitCode
}