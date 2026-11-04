package services

import javax.inject.{Inject, Singleton}

import models.{AuthorView, Grunt, GruntView, NewGrunt, Reader, User}
import repositories.{GruntRepository, UserRepository}

import scala.concurrent.{ExecutionContext, Future}

/** Posting grunts and putting together the lists of them that people read. */
@Singleton
class GruntService @Inject() (grunts: GruntRepository, users: UserRepository)(using ec: ExecutionContext) {
  import GruntService.*

  /** Left is a message for the author. */
  def post(author: User, message: String, friendsOnly: Boolean): Future[Either[String, GruntView]] = {
    val text = message.trim
    if (text.isEmpty) Future.successful(Left("Write something first."))
    else if (text.length > MaxLength) Future.successful(Left(s"A grunt is $MaxLength characters at most."))
    else
      for {
        mentioned <- users.findByHandles(MessageRenderer.mentionedHandles(text))
        mentions = mentioned.map(_.id).filterNot(_ == author.id)
        grunt <- grunts.create(NewGrunt(author.id, text, mentions, System.currentTimeMillis(), friendsOnly))
      } yield Right(render(grunt, Map(author.id -> author)))
  }

  /** Only the author can delete a grunt. Pictures it showed stay stored. */
  def delete(viewer: User, id: String): Future[GruntService.Deletion] =
    grunts.findById(id).flatMap {
      case None => Future.successful(Deletion.NotFound)
      case Some(g) if g.authorId != viewer.id => Future.successful(Deletion.NotYours)
      case Some(g) => grunts.delete(g.id).map(_ => Deletion.Deleted)
    }

  /** The signed-in home page: your grunts, the people you follow, and mentions of you. */
  def homeFeed(viewer: User, before: Option[Long]): Future[Seq[GruntView]] =
    grunts.timeline(viewer.id +: viewer.following, Some(viewer.id), before, PageSize, Reader(viewer)).flatMap(withAuthors)

  /** A profile: their grunts and grunts that mention them. */
  def profileFeed(user: User, before: Option[Long], viewer: Option[User]): Future[Seq[GruntView]] =
    grunts.timeline(Seq(user.id), Some(user.id), before, PageSize, Reader.of(viewer)).flatMap(withAuthors)

  /**
   * What people are looking at: grunts from the last week that someone has seen, ranked by
   * views / (hours old + 2)^1.5. Age divides the views, so a new grunt needs far fewer of
   * them to rank above an old one, and a grunt slides down as it ages unless it keeps
   * being seen.
   */
  def hot(viewer: Option[User]): Future[Seq[GruntView]] = {
    val now = System.currentTimeMillis()
    grunts.viewedSince(now - HotWindowMillis, HotCandidates, Reader.of(viewer))
      .map(_.sortBy(g => -hotScore(g, now)).take(PageSize))
      .flatMap(withAuthors)
  }

  /** The viewer has seen these grunts. Repeat views, and views of your own, don't count. */
  def recordViews(viewer: User, ids: Seq[String]): Future[Unit] =
    if (ids.isEmpty) Future.unit
    else grunts.recordViews(ids.distinct.take(MaxViewBatch), viewer.id, Reader(viewer))

  private def withAuthors(list: Seq[Grunt]): Future[Seq[GruntView]] =
    users.findByIds(list.map(_.authorId).distinct).map { authors =>
      val byId = authors.map(a => a.id -> a).toMap
      list.filter(g => byId.contains(g.authorId)).map(render(_, byId))
    }

  private def render(g: Grunt, authors: Map[String, User]): GruntView =
    GruntView(g.id, AuthorView.from(authors(g.authorId)), MessageRenderer.toHtml(g.message), g.createdAt, g.friendsOnly, g.views)
}

object GruntService {
  enum Deletion { case Deleted, NotFound, NotYours }

  val MaxLength = 1000
  val PageSize = 50
  val MaxViewBatch = 100

  val HotWindowMillis: Long = 7L * 24 * 60 * 60 * 1000
  /** The most recent viewed grunts that are ranked; plenty for a small group's week. */
  val HotCandidates = 1000

  def hotScore(g: Grunt, now: Long): Double = {
    val hours = math.max(0L, now - g.createdAt) / 3600000.0
    g.views / math.pow(hours + 2, 1.5)
  }
}
