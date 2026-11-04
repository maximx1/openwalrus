package services

import javax.inject.{Inject, Singleton}

import play.api.Configuration

import java.util.concurrent.ConcurrentHashMap

/**
 * Anyone can report anonymous views, and a script could make up a new visitor id for each
 * one, so each address may only add so many an hour. Past that, views are quietly dropped.
 * Kept in memory, like AuthThrottle.
 */
@Singleton
class ViewThrottle(perHour: Int) {
  import ViewThrottle.Window

  @Inject() def this(config: Configuration) = this(config.get[Int]("openwalrus.throttle.anonymousViewsPerAddress"))

  /** The address's count and when its hour started. */
  private val counts = new ConcurrentHashMap[String, (Long, Int)]()

  /** How many of `wanted` views the address may still add this hour; counts them as added. */
  def allow(address: String, wanted: Int, now: Long = System.currentTimeMillis()): Int = {
    var granted = 0
    counts.compute(address, (_, old) => {
      val (start, used) = Option(old).filter(_._1 > now - Window).getOrElse((now, 0))
      granted = math.max(0, math.min(wanted, perHour - used))
      (start, used + granted)
    })
    if (counts.size > 10000) counts.entrySet.removeIf(_.getValue._1 <= now - Window)
    granted
  }
}

object ViewThrottle {
  val Window: Long = 60L * 60 * 1000
}
