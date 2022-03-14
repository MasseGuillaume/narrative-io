package narrative

import zio._
import zio.test._

import zhttp.http._
import zio.test.Assertion._

import java.time.OffsetDateTime

import java.nio.charset.StandardCharsets.UTF_8

object ServerSpec extends DefaultRunnableSpec {
  val getServer = 
    EventStore.inMemory.map(store =>
      ServerMain.app.provideEnvironment(ZEnvironment(store))
    )

  def spec = suite("http")(
    testM("empty stats") {
      for {
        server <- getServer
        now <- ZIO.effectTotal(OffsetDateTime.now)
        stats <- getStats(server)(now)
      } yield assert(stats)(equalTo(Stats.empty.toString))
    },
    testM("with events") {
      for {
        server <- getServer
        now <- ZIO.effectTotal(OffsetDateTime.now)
        _ <- appendEvent(server)(now, User.fromString("foo"), Event.Click)
        
        _ <- Clock.ClockLive.sleep(1.second)
        stats <- getStats(server)(now)//.repeatWhile(_ == Stats.empty.toString)
      } yield {
        assert(stats)(
          equalTo(Stats(uniqueUsers = 1L, clicks = 1L, impressions = 0L).toString)
        )
      }
    }
  )

  private def appendEvent(server: UHttpApp)(
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

    server(req).mapError(_ => new Exception("???")).unit
  }

  private def getStats(server: UHttpApp)(time: OffsetDateTime): Task[String] = {
    val req = Request(
      url = URL(
        path = Path() / "analytics",
        queryParams = Map(
          "timestamp" -> List((time.toEpochSecond * 1000).toString)
        )
      )
    )

    server(req)
      .mapError(_ => new Exception("???"))
      .flatMap(_.data.toByteBuf.map(_.toString(UTF_8)))
  }
}
