package narrative

import zhttp.http._
import zhttp.service.Server
import zio._

import java.time.{OffsetDateTime, Instant}
import java.time.ZoneId
import java.lang.NumberFormatException

import scala.util.Try
import java.nio.charset.StandardCharsets.UTF_8

object ServerMain extends zio.App {

  val env =
    ZLayer.fromEffect(EventStore.inMemory)

  val app: Http[EventStore, Nothing, Request, Response] = {
    val analytics = "analytics"

    Http.collectZIO {
      case req @ (Method.GET -> Path() / analytics) =>
        timestamp(req).foldM(
          badRequest,
          date =>
            EventStore(_.stats(date)).map(stats =>
              Response.text(stats.toString, UTF_8)
            )
        )

      case req @ (Method.POST -> Path() / analytics) =>
        val params = timestamp(req).zip(user(req)).zip(event(req))

        params.foldM(
          badRequest,
          (time, user, event) =>
            EventStore(_.append(time, user, event)).map(_ =>
              Response(Status.NO_CONTENT)
            )
        )
    }
  }

  private def badRequest[R, E](msg: String): ZIO[R, E, Response] =
    ZIO.succeed(Response.text(msg).copy(status = Status.BAD_REQUEST))

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

  private def user(request: Request): IO[String, User] =
    param("user")(v => ZIO.succeed(User.fromString(v)))(request)

  private def event(request: Request): IO[String, Event] = {
    val click = "click"
    val impression = "impression"

    param("event") {
      case `click` =>
        ZIO.succeed(Event.Click)
      case `impression` =>
        ZIO.succeed(Event.Impression)
      case other =>
        val values = Event.values.map(_.toString.toLowerCase).mkString(", ")
        ZIO.fail(s"""unknown event type: $other, expected: $values""")
    }(request)
  }

  private def timestamp(request: Request): IO[String, OffsetDateTime] = {
    val paramName = "timestamp"
    param(paramName)(h =>
      ZIO
        .attempt(h.toLong)
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
    Server.start(8090, app.provideSomeLayer(env)).exitCode
}
