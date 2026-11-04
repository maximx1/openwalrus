package services

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.mindrot.jbcrypt.BCrypt

import java.nio.charset.StandardCharsets.UTF_8
import java.security.{MessageDigest, SecureRandom}
import java.util.Base64

/**
 * Password hashes. New ones are Argon2id with OWASP's recommended settings (19 MiB, two
 * passes), in the usual $argon2id$v=19$m=…,t=…,p=…$salt$hash form. Accounts made before
 * have bcrypt hashes: those still check, and AccountService replaces them with Argon2id the
 * next time their owner signs in.
 */
object PasswordHasher {
  val MemoryKiB = 19456
  val Iterations = 2
  val Parallelism = 1
  private val SaltBytes = 16
  private val HashBytes = 32

  private val random = new SecureRandom()
  private val encode = Base64.getEncoder.withoutPadding
  private val decode = Base64.getDecoder
  private val Argon2id = """^\$argon2id\$v=19\$m=(\d{1,7}),t=(\d{1,3}),p=(\d{1,2})\$([A-Za-z0-9+/]+)\$([A-Za-z0-9+/]+)$""".r

  def hash(password: String): String = {
    val salt = new Array[Byte](SaltBytes)
    random.nextBytes(salt)
    val out = derive(password, salt, MemoryKiB, Iterations, Parallelism, HashBytes)
    s"$$argon2id$$v=19$$m=$MemoryKiB,t=$Iterations,p=$Parallelism$$${encode.encodeToString(salt)}$$${encode.encodeToString(out)}"
  }

  def verify(password: String, stored: String): Boolean = stored match {
    case Argon2id(m, t, p, salt, expected) =>
      val want = decode.decode(expected)
      MessageDigest.isEqual(derive(password, decode.decode(salt), m.toInt, t.toInt, p.toInt, want.length), want)
    case bcrypt if bcrypt != null && bcrypt.startsWith("$2") =>
      try BCrypt.checkpw(password, bcrypt)
      catch { case _: IllegalArgumentException => false }
    case _ => false
  }

  /** True for a bcrypt hash, or an Argon2id one made with other settings than today's. */
  def needsRehash(stored: String): Boolean = stored match {
    case Argon2id(m, t, p, _, _) => m.toInt != MemoryKiB || t.toInt != Iterations || p.toInt != Parallelism
    case _ => true
  }

  /**
   * The same work as checking a password, against nothing. Signing in to a handle that
   * doesn't exist does this, so it takes as long as a wrong password for one that does.
   */
  def wasteTime(password: String): Unit = verify(password, decoy)

  private lazy val decoy = hash("there is no such account")

  private def derive(password: String, salt: Array[Byte], memoryKiB: Int, iterations: Int, parallelism: Int, length: Int) = {
    val params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
      .withVersion(Argon2Parameters.ARGON2_VERSION_13)
      .withMemoryAsKB(memoryKiB)
      .withIterations(iterations)
      .withParallelism(parallelism)
      .withSalt(salt)
      .build()
    val generator = new Argon2BytesGenerator()
    generator.init(params)
    val out = new Array[Byte](length)
    generator.generateBytes(password.getBytes(UTF_8), out)
    out
  }
}
