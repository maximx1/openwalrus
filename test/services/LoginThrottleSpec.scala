package services

import org.scalatestplus.play.PlaySpec

class LoginThrottleSpec extends PlaySpec {
  import LoginThrottle.*

  "LoginThrottle" should {
    "block a handle after too many failures, whatever its case" in {
      val t = new LoginThrottle
      (1 to MaxFailures).foreach(_ => t.failed("Wally", now = 1000L))
      t.blocked("@wally", now = 1000L) mustBe true
      t.blocked("tusk", now = 1000L) mustBe false
    }
    "let it in again once the failures are old enough" in {
      val t = new LoginThrottle
      (1 to MaxFailures).foreach(_ => t.failed("wally", now = 1000L))
      t.blocked("wally", now = 1000L + Window + 1) mustBe false
    }
    "forget failures after a successful sign-in" in {
      val t = new LoginThrottle
      (1 until MaxFailures).foreach(_ => t.failed("wally", now = 1000L))
      t.succeeded("wally")
      t.failed("wally", now = 1000L)
      t.blocked("wally", now = 1000L) mustBe false
    }
  }
}
