package services

import javax.inject.{Inject, Singleton}

import play.api.Configuration

import java.util.concurrent.ConcurrentHashMap

/**
 * Slows down password guessing and mass sign-ups. Wrong passwords are counted two ways: by
 * handle, which stops someone guessing at one account from many places, and by address,
 * which stops someone trying a few passwords on every account. Kept in memory, so a
 * restart forgets it.
 *
 * Addresses come from Play, which reads X-Forwarded-For only from the trusted proxies in
 * application.conf. Were the proxy not trusted, everyone would share its address, and
 * the per-address limits would apply to all of them at once.
 */
@Singleton
class AuthThrottle(limits: AuthThrottle.Limits) {
  import AuthThrottle.*

  @Inject() def this(config: Configuration) = this(AuthThrottle.Limits(
    failuresPerAddress = config.get[Int]("openwalrus.throttle.failuresPerAddress"),
    signUpsPerAddress = config.get[Int]("openwalrus.throttle.signUpsPerAddress")
  ))

  private val byHandle = new Counter(MaxFailuresPerHandle, Window)
  private val byAddress = new Counter(limits.failuresPerAddress, Window)
  private val signUps = new Counter(limits.signUpsPerAddress, SignUpWindow)

  def signInBlocked(handle: String, address: String, now: Long = System.currentTimeMillis()): Boolean =
    byHandle.full(key(handle), now) || byAddress.full(address, now)

  def signInFailed(handle: String, address: String, now: Long = System.currentTimeMillis()): Unit = {
    byHandle.add(key(handle), now)
    byAddress.add(address, now)
  }

  /** A right password clears the handle's count; the address's stands, so it can't be reset with an account of one's own. */
  def signInSucceeded(handle: String): Unit = byHandle.clear(key(handle))

  def signUpBlocked(address: String, now: Long = System.currentTimeMillis()): Boolean = signUps.full(address, now)

  def signedUp(address: String, now: Long = System.currentTimeMillis()): Unit = signUps.add(address, now)

  private def key(handle: String) = handle.trim.stripPrefix("@").toLowerCase
}

object AuthThrottle {
  case class Limits(failuresPerAddress: Int, signUpsPerAddress: Int)

  val MaxFailuresPerHandle = 10
  val Window: Long = 15L * 60 * 1000
  val SignUpWindow: Long = 60L * 60 * 1000

  /** Events per key within a sliding window. */
  private class Counter(max: Int, window: Long) {
    private val events = new ConcurrentHashMap[String, Vector[Long]]()

    def full(k: String, now: Long): Boolean = recent(k, now).size >= max

    def add(k: String, now: Long): Unit = {
      events.compute(k, (_, old) => Option(old).getOrElse(Vector.empty).filter(_ > now - window) :+ now)
      // Someone cycling through handles or addresses shouldn't grow this without end.
      if (events.size > 10000) events.entrySet.removeIf(e => e.getValue.forall(_ <= now - window))
    }

    def clear(k: String): Unit = events.remove(k)

    private def recent(k: String, now: Long) = Option(events.get(k)).getOrElse(Vector.empty).filter(_ > now - window)
  }
}
