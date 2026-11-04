package models

import play.api.libs.json.{Json, OWrites}

/** What the JSON API sends back. Never includes password hashes. */
case class UserView(
    id: String,
    handle: String,
    fullName: String,
    profileImage: Option[String],
    bannerImage: Option[String],
    gruntCount: Int,
    followingCount: Int,
    followerCount: Int,
    followedByMe: Boolean
)

object UserView {
  def from(u: User, gruntCount: Int, viewerId: Option[String]): UserView = UserView(
    u.id, u.handle, u.fullName, u.profileImage, u.bannerImage, gruntCount,
    u.following.size, u.followers.size, viewerId.exists(u.followers.contains)
  )
  given OWrites[UserView] = Json.writes[UserView]
}

case class AuthorView(id: String, handle: String, fullName: String, profileImage: Option[String])

object AuthorView {
  def from(u: User): AuthorView = AuthorView(u.id, u.handle, u.fullName, u.profileImage)
  given OWrites[AuthorView] = Json.writes[AuthorView]
}

/** A grunt ready to show: the message already rendered to safe HTML. */
case class GruntView(id: String, author: AuthorView, html: String, createdAt: Long, friendsOnly: Boolean, views: Int)

object GruntView {
  given OWrites[GruntView] = Json.writes[GruntView]
}
