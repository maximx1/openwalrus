package core.utils

/**
 * Type conversion utils and safety.
 */
object TypeUtils {
  
  /**
   * Converts Some(null) to None.
   * @param o The variable to check.
   * @return None or Some(value)
   */
  def nullToNone[T](o: Option[T]) = o match {
    case Some(null) => None
    case Some(x) => Some(x)
    case _ => None
  }
  
  /**
   * Upgrade to Boolean to easily optionify a boolean.
   */
  implicit class RichBoolean(val b: Boolean) extends AnyVal {
    final def option[A](a: => A): Option[A] = if (b) Some(a) else None
  }
}