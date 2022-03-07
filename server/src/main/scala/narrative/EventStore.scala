package narrative

import zio._

import java.time.OffsetDateTime

case class User(userId: String)
enum Event {
  case Click, Impression
}

case class Stats(uniqueUsers: Long, clicks: Long, impressions: Long) {
  final override def toString(): String = 
    s"""|unique_users,$uniqueUsers
        |clicks,$clicks
        |impressions,$impressions""".stripMargin
}


trait EventStore {
  def append(time: OffsetDateTime, user: User, event: Event): UIO[Unit]
  def stats(time: OffsetDateTime): UIO[Stats]
}

object EventStore extends zio.Accessible[EventStore] {
  val inMemory: EventStore = new EventStore {
    def append(time: OffsetDateTime, user: User, event: Event): UIO[Unit] =
      ZIO.unit

    def stats(time: OffsetDateTime): UIO[Stats] =
      ZIO.succeed(Stats(0L, 0L, 0L))
  }
}
