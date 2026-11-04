package services

import org.scalatestplus.play.PlaySpec

class AuthThrottleSpec extends PlaySpec {
  import AuthThrottle.*

  private def throttle() = new AuthThrottle(Limits(failuresPerAddress = 20, signUpsPerAddress = 3))

  "AuthThrottle" should {
    "block a handle after too many failures, whatever its case or address" in {
      val t = throttle()
      (1 to MaxFailuresPerHandle).foreach(i => t.signInFailed("Wally", s"10.0.0.$i", now = 1000L))
      t.signInBlocked("@wally", "10.9.9.9", now = 1000L) mustBe true
      t.signInBlocked("tusk", "10.9.9.9", now = 1000L) mustBe false
    }
    "block an address trying many handles" in {
      val t = throttle()
      (1 to 20).foreach(i => t.signInFailed(s"user$i", "10.0.0.1", now = 1000L))
      t.signInBlocked("someone_else", "10.0.0.1", now = 1000L) mustBe true
      t.signInBlocked("someone_else", "10.0.0.2", now = 1000L) mustBe false
    }
    "let it in again once the failures are old enough" in {
      val t = throttle()
      (1 to MaxFailuresPerHandle).foreach(_ => t.signInFailed("wally", "10.0.0.1", now = 1000L))
      t.signInBlocked("wally", "10.0.0.1", now = 1000L + Window + 1) mustBe false
    }
    "forget a handle's failures after a successful sign-in, but not the address's" in {
      val t = throttle()
      (1 until MaxFailuresPerHandle).foreach(_ => t.signInFailed("wally", "10.0.0.1", now = 1000L))
      t.signInSucceeded("wally")
      t.signInFailed("wally", "10.0.0.2", now = 1000L)
      t.signInBlocked("wally", "10.0.0.2", now = 1000L) mustBe false
      (1 to 11).foreach(_ => t.signInFailed("other", "10.0.0.1", now = 1000L))
      t.signInBlocked("anyone", "10.0.0.1", now = 1000L) mustBe true
    }
    "limit sign-ups per address" in {
      val t = throttle()
      (1 to 3).foreach(_ => t.signedUp("10.0.0.1", now = 1000L))
      t.signUpBlocked("10.0.0.1", now = 1000L) mustBe true
      t.signUpBlocked("10.0.0.2", now = 1000L) mustBe false
      t.signUpBlocked("10.0.0.1", now = 1000L + SignUpWindow + 1) mustBe false
    }
  }
}
