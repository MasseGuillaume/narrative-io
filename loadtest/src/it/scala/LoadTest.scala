

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class LoadTest extends Simulation {

  val httpProtocol = 
    http.baseUrl("http://localhost:8090")

  val basic = 
    scenario("Basic")
      .exec(http("request_1").get("/analytics").queryParam("timestamp", "0"))
      .pause(5)

  setUp(
    basic.inject(
      constantUsersPerSec(1).during(1.second)
    )
  ).protocols(httpProtocol)
}