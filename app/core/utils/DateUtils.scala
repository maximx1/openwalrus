package core.utils

import java.util.Date

/**
  * Utilities to help with dates and times.
  */
object DateUtils {
  /**
    * Milli current timestamp. It's meant to look like the SQL version.
    * @return The current time in milli since the epoch.
    */
  def CURRENT_TIMESTAMP = (new Date()).getTime
}
