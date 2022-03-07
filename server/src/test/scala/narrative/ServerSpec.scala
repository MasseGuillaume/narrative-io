package narrative

import zio._
import zio.test._

import zhttp.http._
import zio.test.Assertion._

import java.time.OffsetDateTime

import java.nio.charset.StandardCharsets.UTF_8

object ServerSpec extends DefaultRunnableSpec {
  val app = ServerMain.app.provideSomeLayer(ServerMain.env)
  
  def spec = suite("http")(
    // testM("empty stats") {
    //   for {
    //     now <- ZIO.effectTotal(OffsetDateTime.now)
    //     stats <- getStats(now)
    //   } yield assert(stats)(equalTo(Stats.empty.toString))
    // },
    testM("with events") {
      for {
        now <- ZIO.effectTotal(OffsetDateTime.now)
        _ <- appendEvent(now, User.fromString("foo"), Event.Click)
        
        _ <- Clock.ClockLive.sleep(1.second)
        stats <- getStats(now)//.repeatWhile(_ == Stats.empty.toString)
      } yield {
        assert(stats)(
          equalTo(Stats(uniqueUsers = 1L, clicks = 1L, impressions = 0L).toString)
        )
      }


    }
  )

  private def appendEvent(
      time: OffsetDateTime,
      user: User,
      event: Event
  ): Task[Unit] = {
    val req = Request(
      method = Method.POST,
      url = URL(
        path = Path() / "analytics",
        queryParams = Map(
          "timestamp" -> List((time.toEpochSecond * 1000).toString),
          "user" -> List(user.toString),
          "event" -> List(event.toString.toLowerCase)
        )
      )
    )

    app(req).mapError(_ => new Exception("???")).unit
  }

  private def getStats(time: OffsetDateTime): Task[String] = {
    val req = Request(
      url = URL(
        path = Path() / "analytics",
        queryParams = Map(
          "timestamp" -> List((time.toEpochSecond * 1000).toString)
        )
      )
    )

    app(req)
      .mapError(_ => new Exception("???"))
      .flatMap(_.data.toByteBuf.map(_.toString(UTF_8)))
  }

}
