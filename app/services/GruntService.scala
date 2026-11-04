package services

import javax.inject.{Inject, Singleton}

import models.{AuthorView, Grunt, GruntView, NewGrunt, User}
import repositories.{GruntRepository, UserRepository}

import scala.concurrent.{ExecutionContext, Future}

/** Posting grunts and putting together the lists of them that people read. */
@Singleton
class GruntService @Inject() (grunts: GruntRepository, users: UserRepository)(using ec: ExecutionContext) {
  import GruntService.*

  /** Left is a message for the author. */
  def post(author: User, message: String): Future[Either[String, GruntView]] = {
    val text = message.trim
    if (text.isEmpty) Future.successful(Left("Write something first."))
    else if (text.length > MaxLength) Future.successful(Left(s"A grunt is $MaxLength characters at most."))
    else
      for {
        mentioned <- users.findByHandles(MessageRenderer.mentionedHandles(text))
        grunt <- grunts.create(NewGrunt(author.id, text, mentioned.map(_.id).filterNot(_ == author.id), System.currentTimeMillis()))
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
    grunts.timeline(viewer.id +: viewer.following, Some(viewer.id), before, PageSize).flatMap(withAuthors)

  /** A profile: their grunts and grunts that mention them. */
  def profileFeed(user: User, before: Option[Long]): Future[Seq[GruntView]] =
    grunts.timeline(Seq(user.id), Some(user.id), before, PageSize).flatMap(withAuthors)

  private def withAuthors(list: Seq[Grunt]): Future[Seq[GruntView]] =
    users.findByIds(list.map(_.authorId).distinct).map { authors =>
      val byId = authors.map(a => a.id -> a).toMap
      list.filter(g => byId.contains(g.authorId)).map(render(_, byId))
    }

  private def render(g: Grunt, authors: Map[String, User]): GruntView =
    GruntView(g.id, AuthorView.from(authors(g.authorId)), MessageRenderer.toHtml(g.message), g.createdAt)
}

object GruntService {
  enum Deletion { case Deleted, NotFound, NotYours }

  val MaxLength = 1000
  val PageSize = 50
}
