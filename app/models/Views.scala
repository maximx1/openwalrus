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

/**
 * A grunt ready to show: the message already rendered to safe HTML. In a feed, regruntedBy
 * is who put it there, and feedAt is when it arrived there (posted, or regrunted).
 */
case class GruntView(
    id: String,
    author: AuthorView,
    html: String,
    createdAt: Long,
    friendsOnly: Boolean,
    views: Int,
    replyTo: Option[String],
    replyCount: Int,
    regruntCount: Int,
    regruntedByMe: Boolean,
    regruntedBy: Option[AuthorView],
    feedAt: Long
)

object GruntView {
  given OWrites[GruntView] = Json.writes[GruntView]
}

/** One page of a feed. next is the `before` that fetches the page after, if there is one. */
case class FeedPage(grunts: Seq[GruntView], next: Option[Long])

object FeedPage {
  given OWrites[FeedPage] = Json.writes[FeedPage]
}

/** A picture or video in a grunt: where it's served, and how big a file it is. */
case class MediaView(kind: String, url: String, bytes: Long, contentType: String)

object MediaView {
  given OWrites[MediaView] = Json.writes[MediaView]
}

/** A site a grunt is embedded on ("" when the browser didn't say), and the views from there. */
case class EmbedSite(site: String, views: Int)

object EmbedSite {
  given OWrites[EmbedSite] = Json.writes[EmbedSite]
}

/** The numbers behind a grunt, for its Stats dialog. */
case class GruntStats(
    views: Int,
    signedInViews: Int,
    anonymousViews: Int,
    embedViews: Int,
    embeds: Seq[EmbedSite],
    replies: Int,
    regrunts: Int,
    characters: Int,
    media: Seq[MediaView]
)

object GruntStats {
  given OWrites[GruntStats] = Json.writes[GruntStats]
}
