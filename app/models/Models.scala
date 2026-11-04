package models

/**
 * The domain model. Ids are plain strings so nothing above the repositories knows which
 * database they came from.
 */
case class User(
    id: String,
    handle: String,
    fullName: String,
    passwordHash: String,
    createdAt: Long,
    profileImage: Option[String],
    bannerImage: Option[String],
    following: Seq[String],
    followers: Seq[String]
)

case class NewUser(handle: String, fullName: String, passwordHash: String, createdAt: Long)

/**
 * A post. The message is kept as the Markdown the author typed; it is rendered to HTML on
 * the way out. Mentions are the ids of the users it @-mentions, so it shows on their
 * profiles as well. A friends-only grunt is seen by its author and the people they follow.
 * Views is how many people (other than the author) have seen it.
 *
 * Two other kinds are stored the same way. A reply has `replyTo`, the grunt it answers;
 * it's shown under that grunt, not in feeds, and whoever can see that grunt can see it.
 * A regrunt has `regruntOf` and no message: it puts that grunt into the feeds of the
 * regrunter's followers, as of when it was regrunted.
 */
case class Grunt(
    id: String,
    authorId: String,
    message: String,
    mentions: Seq[String],
    createdAt: Long,
    friendsOnly: Boolean,
    views: Int,
    replyTo: Option[String] = None,
    regruntOf: Option[String] = None,
    replyCount: Int = 0,
    regruntCount: Int = 0
)

case class NewGrunt(
    authorId: String,
    message: String,
    mentions: Seq[String],
    createdAt: Long,
    friendsOnly: Boolean,
    replyTo: Option[String] = None
)

/**
 * Someone reading grunts, as far as friends-only ones go: the authors whose friends-only
 * grunts they may see. An author's friends are the people they follow, so that's the reader
 * and everyone who follows them. Signed out, nobody's.
 */
case class Reader(friendOf: Seq[String])

object Reader {
  val SignedOut: Reader = Reader(Seq.empty)
  def apply(user: User): Reader = Reader(user.id +: user.followers)
  def of(user: Option[User]): Reader = user.fold(SignedOut)(apply)
}

/**
 * A signed-in browser. Its token lives only in that browser's cookie; what's stored is a
 * hash of it, so a copy of the database can't be used to sign in. It ends when unused for
 * a while (expiresAt, pushed back as it's used), when it's old enough, or when signed out.
 */
case class SessionRecord(
    id: String,
    userId: String,
    createdAt: Long,
    lastSeenAt: Long,
    expiresAt: Long,
    userAgent: String,
    address: String
)

case class NewSession(tokenHash: String, userId: String, createdAt: Long, expiresAt: Long, userAgent: String, address: String)

/** An image's bytes as stored, with the type to serve it as. */
case class StoredImage(bytes: Array[Byte], contentType: String)
