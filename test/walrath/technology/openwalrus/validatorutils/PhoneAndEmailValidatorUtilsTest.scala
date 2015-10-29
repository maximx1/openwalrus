package walrath.technology.openwalrus.validatorutils
import walrath.technology.openwalrus.testing.BaseTestSpec


class PhoneAndEmailValidatorUtilsTest extends BaseTestSpec {
  "A Phone Validator" should {
    "return true for valid digit only number with a length of 10" in {
      PhoneAndEmailValidatorUtils.checkIfPhoneNumber("1236547890") shouldBe true
    }
    
    "return false for invalid supposedly 10 digit only numbers" in {
      PhoneAndEmailValidatorUtils.checkIfPhoneNumber("02365478900") shouldBe false
      PhoneAndEmailValidatorUtils.checkIfPhoneNumber("236547890") shouldBe false
      PhoneAndEmailValidatorUtils.checkIfPhoneNumber("12365 4780") shouldBe false
      PhoneAndEmailValidatorUtils.checkIfPhoneNumber("1236547f90") shouldBe false
    }
    
    "return true for valid digit only number with a length of 11" in {
      PhoneAndEmailValidatorUtils.checkIfPhoneNumber("11236547890") shouldBe true
    }
    
    "return false for invalid supposedly 11 digit only numbers" in {
      PhoneAndEmailValidatorUtils.checkIfPhoneNumber("02365478900") shouldBe false
      PhoneAndEmailValidatorUtils.checkIfPhoneNumber("236547890") shouldBe false
      PhoneAndEmailValidatorUtils.checkIfPhoneNumber("1236547 890") shouldBe false
      PhoneAndEmailValidatorUtils.checkIfPhoneNumber("1236547f906") shouldBe false
    }
    
    "return true for valid domestic phone number string" in {
      PhoneAndEmailValidatorUtils.checkIfPhoneNumber("(123) 555-1234") shouldBe true
      PhoneAndEmailValidatorUtils.checkIfPhoneNumber("(123)555-1234") shouldBe true
    }
    
    "return false for invalid supposed domestic phone number string" in {
      PhoneAndEmailValidatorUtils.checkIfPhoneNumber("(123 555-1234") shouldBe false
      PhoneAndEmailValidatorUtils.checkIfPhoneNumber("(123) 5551234") shouldBe false
    }
    
    "return true for valid international phone number string" in {
      PhoneAndEmailValidatorUtils.checkIfPhoneNumber("+1-123-555-1234") shouldBe true
    }
    
    "return false for invalid supposed international phone number string" in {
      PhoneAndEmailValidatorUtils.checkIfPhoneNumber("+1123-555-1234") shouldBe false
      PhoneAndEmailValidatorUtils.checkIfPhoneNumber("+1123555-1234") shouldBe false
      PhoneAndEmailValidatorUtils.checkIfPhoneNumber("+11235551234") shouldBe false
      PhoneAndEmailValidatorUtils.checkIfPhoneNumber("1123-555-12349") shouldBe false
    }
    
    "return true for valid dashed phone number" in {
      PhoneAndEmailValidatorUtils.checkIfPhoneNumber("123-555-1234") shouldBe true
    }
    
    "return false for invalid supposed dashed phone number" in {
      PhoneAndEmailValidatorUtils.checkIfPhoneNumber("1232-555-1234") shouldBe false
      PhoneAndEmailValidatorUtils.checkIfPhoneNumber("122555-1234") shouldBe false
    }
    
    "should convert any of the valid phone numbers into the domestic format" in {
      val expected = Some("(432) 123-5555")
      PhoneAndEmailValidatorUtils.convertToDomesticPhone("+1-432-123-5555") shouldBe expected
      PhoneAndEmailValidatorUtils.convertToDomesticPhone("432-123-5555") shouldBe expected
      PhoneAndEmailValidatorUtils.convertToDomesticPhone("(432) 123-5555") shouldBe expected
      PhoneAndEmailValidatorUtils.convertToDomesticPhone("(432)123-5555") shouldBe expected
      PhoneAndEmailValidatorUtils.convertToDomesticPhone("4321235555") shouldBe expected
      PhoneAndEmailValidatorUtils.convertToDomesticPhone("14321235555") shouldBe expected
    }
    
    "should return None while converting invalid phone numbers" in {
      PhoneAndEmailValidatorUtils.convertToDomesticPhone("1234") shouldBe None
    }
  }
  
  "An email validator" should {
    "return true if the string looks somewhat like an email" in {
      PhoneAndEmailValidatorUtils.checkIfPossiblyEmail("abc@def.com") shouldBe true
    }
    
    "return false if the string doesn't look like it could be an email" in {
      PhoneAndEmailValidatorUtils.checkIfPossiblyEmail("123-321-5555") shouldBe false
    }
  }
}