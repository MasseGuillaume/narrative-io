package narrative

import zio._
import zio.test._

import zio.test.Assertion._

import java.time.OffsetDateTime

object EventStoreSpec extends DefaultRunnableSpec {

  private val env = ZLayer.fromEffect(EventStore.inMemory)

  def spec = suite("event-store")(
    testM("with events") {
      val user1 = User.fromString("foo")
      val user2 = User.fromString("bar")

      val app = 
        for {
          now <- ZIO.effectTotal(OffsetDateTime.now)
          twoHoursAgo = now.minusHours(2)
        
          // should be ignored
          _ <- EventStore(_.append(twoHoursAgo, user1, Event.Impression))

          // on the hour
          _ <- EventStore(_.append(now, user1, Event.Click))
          _ <- EventStore(_.append(now, user2, Event.Impression))
          _ <- EventStore(_.append(now, user2, Event.Click))

          out <- EventStore(_.stats(now))

        } yield assert(out)(equalTo(Stats(uniqueUsers = 2, clicks = 2, impressions = 1)))

      app.provide(env)
    }
  )
}