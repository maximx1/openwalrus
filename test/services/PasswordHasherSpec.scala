package services

import org.mindrot.jbcrypt.BCrypt
import org.scalatestplus.play.PlaySpec

class PasswordHasherSpec extends PlaySpec {
  "PasswordHasher" should {
    "make Argon2id hashes that check the right password and no other" in {
      val h = PasswordHasher.hash("correct horse battery")
      h must startWith("$argon2id$v=19$m=19456,t=2,p=1$")
      PasswordHasher.verify("correct horse battery", h) mustBe true
      PasswordHasher.verify("correct horse batterY", h) mustBe false
      PasswordHasher.needsRehash(h) mustBe false
    }
    "salt every hash" in {
      PasswordHasher.hash("same password") must not equal PasswordHasher.hash("same password")
    }
    "still check the bcrypt hashes of older accounts, and want them replaced" in {
      val old = BCrypt.hashpw("from 2015", BCrypt.gensalt(4))
      PasswordHasher.verify("from 2015", old) mustBe true
      PasswordHasher.verify("from 2016", old) mustBe false
      PasswordHasher.needsRehash(old) mustBe true
    }
    "want a hash made with other settings replaced" in {
      PasswordHasher.needsRehash("$argon2id$v=19$m=4096,t=3,p=1$c2FsdHNhbHRzYWx0$aGFzaGhhc2hoYXNo") mustBe true
    }
    "refuse anything that isn't a hash" in {
      PasswordHasher.verify("x", "") mustBe false
      PasswordHasher.verify("x", "plain text") mustBe false
      PasswordHasher.verify("x", null) mustBe false
    }
  }
}
