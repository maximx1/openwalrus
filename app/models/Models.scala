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
 * profiles as well.
 */
case class Grunt(
    id: String,
    authorId: String,
    message: String,
    mentions: Seq[String],
    createdAt: Long
)

case class NewGrunt(authorId: String, message: String, mentions: Seq[String], createdAt: Long)

/** An image's bytes as stored, with the type to serve it as. */
case class StoredImage(bytes: Array[Byte], contentType: String)
