package services

import javax.inject.{Inject, Singleton}

import models.{NewSession, SessionRecord, User}
import repositories.{SessionRepository, UserRepository}

import java.nio.charset.StandardCharsets.UTF_8
import java.security.{MessageDigest, SecureRandom}
import java.util.{Base64, HexFormat}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Signed-in sessions, kept in the database so they can be listed and ended: signing out
 * ends the session for good (not just in that browser), and a new password ends every
 * other one. The browser's cookie carries only a random token.
 */
@Singleton
class SessionService @Inject() (sessions: SessionRepository, users: UserRepository)(using ec: ExecutionContext) {
  import SessionService.*

  /** Starts a session and returns its token, for the cookie. */
  def start(user: User, userAgent: String, address: String, now: Long = System.currentTimeMillis()): Future[String] = {
    val token = newToken()
    sessions.create(NewSession(digest(token), user.id, now, now + IdleTimeout, userAgent.take(300), address)).map(_ => token)
  }

  /** The live session for a token and whose it is. A used session's idle clock restarts (noted every few minutes). */
  def resolve(token: String, now: Long = System.currentTimeMillis()): Future[Option[(SessionRecord, User)]] =
    sessions.findByTokenHash(digest(token)).flatMap {
      case Some(s) if live(s, now) =>
        users.findById(s.userId).flatMap {
          case Some(user) =>
            val touched = if (now - s.lastSeenAt >= TouchEvery) sessions.touch(s.id, now, now + IdleTimeout) else Future.unit
            touched.map(_ => Some(s -> user))
          case None => Future.successful(None)
        }
      case Some(s) => sessions.delete(s.userId, s.id).map(_ => None)
      case None => Future.successful(None)
    }

  /** The user's live sessions, most recently used first. */
  def list(user: User, now: Long = System.currentTimeMillis()): Future[Seq[SessionRecord]] =
    sessions.forUser(user.id).map(_.filter(live(_, now)).sortBy(-_.lastSeenAt))

  def end(user: User, sessionId: String): Future[Boolean] = sessions.delete(user.id, sessionId)

  def endAllBut(user: User, keep: Option[String]): Future[Unit] = sessions.deleteAll(user.id, keep)

  private def live(s: SessionRecord, now: Long) = now < s.expiresAt && now - s.createdAt < MaxAge
}

object SessionService {
  /** Unused this long, a session ends. */
  val IdleTimeout: Long = 30L * 24 * 60 * 60 * 1000
  /** However much it's used, a session ends this long after sign-in. */
  val MaxAge: Long = 90L * 24 * 60 * 60 * 1000
  /** How often a session in use is written back as used, rather than on every request. */
  val TouchEvery: Long = 5L * 60 * 1000

  private val random = new SecureRandom()

  /** 256 random bits. */
  def newToken(): String = {
    val bytes = new Array[Byte](32)
    random.nextBytes(bytes)
    Base64.getUrlEncoder.withoutPadding.encodeToString(bytes)
  }

  /** The token is random and long, so a plain SHA-256 is enough to make the stored copy useless. */
  def digest(token: String): String =
    HexFormat.of.formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(UTF_8)))
}
