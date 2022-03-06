import zio.test._
import zhttp.http._
import zio.test.Assertion._

object Spec extends DefaultRunnableSpec {
  val app = ServerMain.app
  
  def spec = suite("http") (
    testM("should be ok") {
      val req = Request(
        url = URL(path = Path() / "text")
      )
      assertM(app(req).map(_.status))(equalTo(Status.OK))
    }
  )
}