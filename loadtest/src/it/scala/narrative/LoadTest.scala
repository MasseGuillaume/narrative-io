package narrative

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import java.time.{OffsetDateTime, Instant, ZoneId}

import scala.concurrent.duration._
import scala.util.Random
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit

class LoadTest extends Simulation {

  val httpProtocol = http.baseUrl("http://localhost:8090")

  val analyticsPath = "/analytics"

  val userCount = new AtomicInteger(0)
  def getUserName: String = {
    val userId = userCount.getAndIncrement
    s"user-$userId"
  }

  val current = System.currentTimeMillis
  def delta: Long = System.currentTimeMillis - current

  // We want 95% of the traffic within the last hour (~2 STD)
  def startTime: Long = {
    // TODO double check the math here
    val oneHourInMillis = TimeUnit.HOURS.toMillis(1L)
    delta - Math.floor(Math.abs(Random.nextGaussian) * oneHourInMillis / 2L).toLong
  }

  // Someone looking at the latest analytics trends
  val analyist = 
    scenario("Analyst")
      .exec(_.set("timestamp", startTime.toString))
      .exec(
        http("get stats")
          .get(analyticsPath)
          .queryParam("timestamp", "#{timestamp}")
      )
      .pause(5)

  // Users generating logs
  val webusers =
    scenario("Web Users")
      .exec(
        _.set("name", getUserName)
         .set("timestamp", startTime.toString)
      )
      .exec(
        http("impression")
          .post(analyticsPath)
          .queryParamMap(
            Map(
              "timestamp" -> "#{timestamp}",
              "user" -> "#{name}",
              "event" -> "impression"
            )
          )
      )
      .pause(100.millis, 1.second)
      .repeat(10)(
        exec(_.set("timestamp", delta))
        .exec(
          http("click")
            .post(analyticsPath)
            .queryParamMap(
              Map(
                "timestamp" ->"#{timestamp}",
                "user" -> "#{name}",
                "event" -> "click"
              )
            )
        )
        .pause(100.millis, 200.millis)
      )

  setUp(
    analyist.inject(
      nothingFor(30.minutes),
      constantUsersPerSec(10).during(1.hour)
    ),
    webusers.inject(
      constantUsersPerSec(1000).during(1.hour)
    )
  ).protocols(httpProtocol)
}