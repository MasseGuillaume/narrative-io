package narrative

import zio._

import java.time.OffsetDateTime

opaque type User = String
object User {
  def fromString(user: String): User = user
}

enum Event {
  case Click, Impression
}

case class Stats(uniqueUsers: Long, clicks: Long, impressions: Long) {
  final override def toString(): String = 
    s"""|unique_users,$uniqueUsers
        |clicks,$clicks
        |impressions,$impressions""".stripMargin
}

object Stats {
  def empty: Stats = Stats(0L, 0L, 0L)
}

trait EventStore {
  def append(time: OffsetDateTime, user: User, event: Event): UIO[Unit]
  def stats(time: OffsetDateTime): UIO[Stats]
}

object EventStore extends zio.Accessible[EventStore] {
  val inMemory: Task[EventStore] = {
    Ref.make(List.empty[(OffsetDateTime, User, Event)]).map(log =>
      new EventStore {
        def append(time: OffsetDateTime, user: User, event: Event): UIO[Unit] =
          log.update((time, user, event) :: _).unit
          
        def stats(time: OffsetDateTime): UIO[Stats] = {
          def overlapHour(other: OffsetDateTime): Boolean = {
            time.getYear == other.getYear &&
            time.getMonth == other.getMonth &&
            time.getDayOfMonth == other.getDayOfMonth &&
            time.getHour == other.getHour
          }

          log.get.map{events =>
            events
              .filter(e => overlapHour(e._1))
              .foldLeft((Set.empty[User], Stats.empty)) {
                case ((knownUsers, stats), (_, user, event)) =>

                  (
                    knownUsers + user,
                    stats.copy(
                      uniqueUsers = stats.uniqueUsers + (if (!knownUsers.contains(user)) 1 else 0),
                      clicks = stats.clicks + (if (event == Event.Click) 1 else 0),
                      impressions = stats.impressions + (if (event == Event.Impression) 1 else 0)
                    )
                  )
              }._2
          }
        }
      }
    )
  }
}
