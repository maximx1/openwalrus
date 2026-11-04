import org.scalatestplus.play.PlaySpec

class OpenwalrusLoaderSpec extends PlaySpec {
  "fromAppUrl" should {
    "allow the address's host and mark cookies Secure for https" in {
      val c = OpenwalrusLoader.fromAppUrl(Some("https://walrus.example.com"))
      c.get[Seq[String]]("play.filters.hosts.allowed") must contain("walrus.example.com")
      c.get[Boolean]("play.http.session.secure") mustBe true
      c.get[String]("play.http.session.cookieName") must startWith("__Host-")
    }
    "leave cookies usable over plain http" in {
      OpenwalrusLoader.fromAppUrl(Some("http://localhost:9000")).get[Boolean]("play.http.session.secure") mustBe false
    }
    "change nothing without APP_URL" in {
      OpenwalrusLoader.fromAppUrl(None).keys mustBe empty
    }
  }
}
