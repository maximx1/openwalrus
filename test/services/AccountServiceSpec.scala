package services

import org.scalatestplus.play.PlaySpec

class AccountServiceSpec extends PlaySpec {
  import AccountService.validate

  "validate" should {
    "accept a normal sign-up" in {
      validate("Justin", "justin_w", "longenough") mustBe None
    }
    "refuse handles that aren't word characters" in {
      validate("Justin", "jus tin", "longenough") mustBe defined
      validate("Justin", "", "longenough") mustBe defined
    }
    "refuse handles the web app uses as pages" in {
      validate("Justin", "Team", "longenough") mustBe defined
    }
    "refuse short passwords" in {
      validate("Justin", "justin", "short") mustBe defined
      validate("Justin", "justin", "ninechars") mustBe defined
    }
    "refuse passwords that are easy to guess" in {
      validate("Justin", "justin_w", "Password123") mustBe defined
      validate("Justin", "justin_w", "1212121212") mustBe defined
      validate("Justin", "justin_w", "my justin_w password") mustBe defined
    }
  }
}
