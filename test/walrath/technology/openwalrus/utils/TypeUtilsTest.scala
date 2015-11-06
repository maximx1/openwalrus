package walrath.technology.openwalrus.utils

import walrath.technology.openwalrus.testing.BaseTestSpec

class TypeUtilsTest extends BaseTestSpec {
  "Null conversion" should {
    "convert to None when contained in an option" in {
      TypeUtils.nullToNone(Some(null)) shouldBe None
    }
    
    "stay option wrapped type when not null" in {
      val expected = Some("Snicklefritz")
      TypeUtils.nullToNone(expected) shouldBe expected
    }
    
    "return None if passed in None" in {
      TypeUtils.nullToNone(None) shouldBe None
    }
    
    "convert to None when null" in {
      TypeUtils.nullToNone(null) shouldBe None
    }
  }
}