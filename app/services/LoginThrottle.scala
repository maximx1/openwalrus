package services

import javax.inject.Singleton

import java.util.concurrent.ConcurrentHashMap

/**
 * Slows down password guessing: after MaxFailures wrong passwords for one handle within
 * Window, that handle can't be signed in to until the oldest of them is Window old.
 * Kept in memory, so a restart forgets it. Keyed by handle rather than address, because
 * behind a reverse proxy every visitor has the proxy's address.
 */
@Singleton
class LoginThrottle {
  import LoginThrottle.*

  private val failures = new ConcurrentHashMap[String, Vector[Long]]()

  def blocked(handle: String, now: Long = System.currentTimeMillis()): Boolean =
    recent(key(handle), now).size >= MaxFailures

  def failed(handle: String, now: Long = System.currentTimeMillis()): Unit =
    failures.compute(key(handle), (_, old) => Option(old).getOrElse(Vector.empty).filter(_ > now - Window) :+ now)

  def succeeded(handle: String): Unit = failures.remove(key(handle))

  private def recent(k: String, now: Long): Vector[Long] =
    Option(failures.get(k)).getOrElse(Vector.empty).filter(_ > now - Window)

  private def key(handle: String) = handle.trim.stripPrefix("@").toLowerCase
}

object LoginThrottle {
  val MaxFailures = 10
  val Window: Long = 15L * 60 * 1000
}
