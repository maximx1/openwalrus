package core.utils

import core.BaseTestSpec

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
  
  "A RichBoolean" should {
    import core.utils.TypeUtils.RichBoolean
    
    "place a value in option if true" in {
      val trueBool = true
      trueBool.option(1) shouldBe Some(1)
    }
    
    "give an empty option if false" in {
      val falseBool = false
      falseBool.option(1) shouldBe None
    }
  }
}