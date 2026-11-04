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
 */
case class Grunt(
    id: String,
    authorId: String,
    message: String,
    mentions: Seq[String],
    createdAt: Long,
    friendsOnly: Boolean,
    views: Int
)

case class NewGrunt(authorId: String, message: String, mentions: Seq[String], createdAt: Long, friendsOnly: Boolean)

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
