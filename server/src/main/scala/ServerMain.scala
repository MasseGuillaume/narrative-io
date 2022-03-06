import zhttp.http._
import zhttp.service.Server
import zio._

import java.time.{OffsetDateTime, Instant}
import java.time.ZoneId
import java.lang.NumberFormatException

import scala.util.Try

object ServerMain extends zio.App {

  val app: UHttpApp = {
    val analytics = "analytics"

    Http.collectZIO {
      case req @ (Method.GET -> Path() / analytics) =>
        timestamp(req).fold(
          msg => Response.text(msg).copy(status = Status.BAD_REQUEST),
          date => Response.text(date.toString)
        )

      case req @ (Method.POST -> Path() / analytics) =>
        val params = timestamp(req).zip(user(req)).zip(event(req))
        
        params.fold(
          msg => Response.text(msg).copy(status = Status.BAD_REQUEST),
          (time, user, event) => {
            Response.text(time.toString)
          }
        )
    }
  }

  private def param[T](
      paramName: String
  )(f: String => IO[String, T])(request: Request): IO[String, T] = {
    ZIO
      .fromOption(request.url.queryParams.get(paramName))
      .mapError(_ => s"missing query parameter $paramName")
      .flatMap {
        case h :: Nil => f(h)
        case e =>
          ZIO.fail(s"expected only one query parameter $paramName, got: $e")
      }
  }

  case class User(userId: String)
  private def user(request: Request): IO[String, User] =
    param("user")(v => ZIO.succeed(User(v)))(request)

  enum Event {
    case Click, Impression
  }

  private def event(request: Request): IO[String, Event] = {
    val click = "click"
    val impression = "impression"

    param("user") {
      case `click`      => ZIO.succeed(Event.Click)
      case `impression` => ZIO.succeed(Event.Impression)
      case other =>
        ZIO.fail(s"""unknown event type: $other, expected: ${Event.values
          .mkString(", ")}""")
    }(request)
  }

  private def timestamp(request: Request): IO[String, OffsetDateTime] = {
    val paramName = "timestamp"
    param(paramName)(h =>
      ZIO
        .succeed(h)
        .mapAttempt(_.toLong)
        .mapError { case e: NumberFormatException =>
          s"query parameter $paramName, expected number got $h"
        }
        .map(millis =>
          OffsetDateTime
            .ofInstant(Instant.ofEpochMilli(millis), ZoneId.of("GMT"))
        )
    )(request)
  }

  final def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    Server.start(8090, app).exitCode
}
