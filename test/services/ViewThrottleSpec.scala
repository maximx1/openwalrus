package services

import org.scalatestplus.play.PlaySpec

class ViewThrottleSpec extends PlaySpec {
  private val hour = ViewThrottle.Window

  "ViewThrottle" should {
    "let each address add only so many views an hour" in {
      val throttle = new ViewThrottle(10)
      throttle.allow("1.2.3.4", 6, now = 0) mustBe 6
      throttle.allow("1.2.3.4", 6, now = 1) mustBe 4
      throttle.allow("1.2.3.4", 1, now = 2) mustBe 0
      throttle.allow("5.6.7.8", 3, now = 2) mustBe 3
      throttle.allow("1.2.3.4", 6, now = hour + 1) mustBe 6
    }
  }
}
