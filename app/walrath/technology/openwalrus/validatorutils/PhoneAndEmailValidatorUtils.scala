package walrath.technology.openwalrus.validatorutils

import scala.util.matching.Regex

/**
 * Utilities to help validate strings for phones or emails.
 * US standard...
 */
object PhoneAndEmailValidatorUtils {
  /**
   * Common US phone number valid formats.
   */
  private val validPhoneNumbers = Map[String, String](
      "allDigitsNoCountry" -> "^\\d{10}$",
      "allDigitsWithCountry" -> "^1\\d{10}$",
      "domesticFormat" -> "^\\(\\d{3}\\)\\s?\\d{3}-\\d{4}$",
      "international" -> "^\\+\\d-\\d{3}-\\d{3}-\\d{4}$",
      "basicDashedGroup" -> "^\\d{3}-\\d{3}-\\d{4}$"
  )
  
  /**
   * Match grouped editions of the validations above to convert to domestic format.
   */
  private val phoneNumberConversions = Map[String, Regex](
      "allDigitsNoCountry" -> "^(\\d{3})(\\d{3})(\\d{4})$".r,
      "allDigitsWithCountry" -> "^\\d(\\d{3})(\\d{3})(\\d{4})$".r,
      "domesticFormat" -> "^\\((\\d{3})\\)\\s?(\\d{3})-(\\d{4})$".r,
      "international" -> "^\\+\\d-(\\d{3})-(\\d{3})-(\\d{4})$".r,
      "basicDashedGroup" -> "^(\\d{3})-(\\d{3})-(\\d{4})$".r
  )
  
  /**
   * Checks if the string is a phone number.
   * @param str The string to validate.
   * @return true if a US phone number.
   */
  def checkIfPhoneNumber(str: String): Boolean = !validPhoneNumbers.values.filter(str.matches(_)).isEmpty
  
  /**
   * Checks if the string is potentially an email.
   * @param str The string to validate.
   * @return true if the string looks sort of like an email.
   */
  def checkIfPossiblyEmail(str: String) = str.matches("^.*@.*$")
  
  /**
   * Converts a phone number to a domestic phone number.
   */
  def convertToDomesticPhone(str: String): Option[String] = {
    val key = phoneNumberConversions.keys.filter(x => str.matches(validPhoneNumbers(x))).headOption
    val matchGroup = key.flatMap(phoneNumberConversions(_).findFirstMatchIn(str))
    return matchGroup.map(x => "(" + x.group(1) + ") " + x.group(2) + "-" + x.group(3))
  }
}