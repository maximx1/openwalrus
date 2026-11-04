package services

import javax.inject.{Inject, Singleton}

import models.{AuthorView, FeedPage, Grunt, GruntView, NewGrunt, Reader, User}
import repositories.{GruntRepository, UserRepository}

import scala.concurrent.{ExecutionContext, Future}

/** Posting, replying and regrunting, and putting together the lists of grunts people read. */
@Singleton
class GruntService @Inject() (grunts: GruntRepository, users: UserRepository)(using ec: ExecutionContext) {
  import GruntService.*

  /** Left is a message for the author. */
  def post(author: User, message: String, friendsOnly: Boolean): Future[Either[String, GruntView]] =
    create(author, message, friendsOnly, replyTo = None)

  /** A reply goes under the grunt, which must be one the viewer can see (and not itself a reply). */
  def reply(viewer: User, gruntId: String, message: String): Future[Either[Problem, GruntView]] =
    findVisible(gruntId, Some(viewer)).flatMap {
      case None => Future.successful(Left(Problem.NotFound))
      case Some(g) if g.replyTo.isDefined => Future.successful(Left(Problem.Invalid("Reply to the grunt itself, not to a reply.")))
      case Some(g) => create(viewer, message, friendsOnly = false, replyTo = Some(g.id)).map(_.left.map(Problem.Invalid(_)))
    }

  private def create(author: User, message: String, friendsOnly: Boolean, replyTo: Option[String]): Future[Either[String, GruntView]] = {
    val text = message.trim
    if (text.isEmpty) Future.successful(Left("Write something first."))
    else if (text.length > MaxLength) Future.successful(Left(s"A grunt is $MaxLength characters at most."))
    else
      for {
        mentioned <- users.findByHandles(MessageRenderer.mentionedHandles(text))
        mentions = mentioned.map(_.id).filterNot(_ == author.id)
        grunt <- grunts.create(NewGrunt(author.id, text, mentions, System.currentTimeMillis(), friendsOnly, replyTo))
        shown <- present(Seq(grunt), Some(author))
      } yield Right(shown.head)
  }

  /** Only the author can delete a grunt; its replies and regrunts go with it. Pictures it showed stay stored. */
  def delete(viewer: User, id: String): Future[GruntService.Deletion] =
    grunts.findById(id).flatMap {
      case None => Future.successful(Deletion.NotFound)
      case Some(g) if g.regruntOf.isDefined => Future.successful(Deletion.NotFound)
      case Some(g) if g.authorId != viewer.id => Future.successful(Deletion.NotYours)
      case Some(g) => grunts.delete(g.id).map(_ => Deletion.Deleted)
    }

  /** One grunt or reply, for its own page. */
  def one(id: String, viewer: Option[User]): Future[Option[GruntView]] =
    findVisible(id, viewer).flatMap(_.fold(Future.successful(None))(g => present(Seq(g), viewer).map(_.headOption)))

  /** A grunt's replies, oldest first. None if the grunt isn't there for the viewer. */
  def replies(id: String, viewer: Option[User]): Future[Option[Seq[GruntView]]] =
    findVisible(id, viewer).flatMap {
      case Some(g) if g.replyTo.isEmpty => grunts.replies(g.id, MaxReplies).flatMap(present(_, viewer)).map(Some(_))
      case _ => Future.successful(None)
    }

  /**
   * Puts the grunt in your followers' feeds, or takes it back out. Not your own, not a
   * reply, and not a friends-only one, which would show it to people outside its friends.
   */
  def setRegrunt(viewer: User, id: String, on: Boolean): Future[Either[Problem, GruntView]] =
    findVisible(id, Some(viewer)).flatMap {
      case None => Future.successful(Left(Problem.NotFound))
      case Some(g) if g.replyTo.isDefined => Future.successful(Left(Problem.Invalid("Replies can't be regrunted.")))
      case Some(g) if g.authorId == viewer.id => Future.successful(Left(Problem.Invalid("That's your own grunt.")))
      case Some(g) if on && g.friendsOnly => Future.successful(Left(Problem.NotAllowed("Friends-only grunts can't be regrunted.")))
      case Some(g) =>
        val change = if (on) grunts.regrunt(viewer.id, g.id, System.currentTimeMillis()) else grunts.unregrunt(viewer.id, g.id)
        change.flatMap(_ => one(g.id, Some(viewer))).map(_.toRight(Problem.NotFound))
    }

  /** The author can make a grunt friends-only after posting it, or open it up again. */
  def setFriendsOnly(viewer: User, id: String, friendsOnly: Boolean): Future[Either[Problem, GruntView]] =
    grunts.findById(id).flatMap {
      case Some(g) if g.regruntOf.isEmpty =>
        if (g.authorId != viewer.id) Future.successful(Left(Problem.NotAllowed("Only its author can change who sees it.")))
        else if (g.replyTo.isDefined) Future.successful(Left(Problem.Invalid("A reply is seen by whoever can see the grunt it answers.")))
        else grunts.setFriendsOnly(g.id, friendsOnly).flatMap(_ => one(g.id, Some(viewer))).map(_.toRight(Problem.NotFound))
      case _ => Future.successful(Left(Problem.NotFound))
    }

  /** The signed-in home page: your grunts, the people you follow (and what they regrunt), and mentions of you. */
  def homeFeed(viewer: User, before: Option[Long]): Future[FeedPage] =
    page(grunts.timeline(viewer.id +: viewer.following, Some(viewer.id), before, PageSize, Reader(viewer)), Some(viewer))

  /** A profile: their grunts and regrunts, and grunts that mention them. */
  def profileFeed(user: User, before: Option[Long], viewer: Option[User]): Future[FeedPage] =
    page(grunts.timeline(Seq(user.id), Some(user.id), before, PageSize, Reader.of(viewer)), viewer)

  /**
   * What's getting traction: grunts from the last week that people have seen, answered or
   * regrunted, ranked by hotScore. Age divides the score, so a new grunt needs far less
   * to rank above an old one, and a grunt slides down as it ages unless it keeps going.
   */
  def hot(viewer: Option[User]): Future[Seq[GruntView]] = {
    val now = System.currentTimeMillis()
    grunts.activeSince(now - HotWindowMillis, HotCandidates, Reader.of(viewer))
      .map(_.sortBy(g => -hotScore(g, now)).take(PageSize))
      .flatMap(present(_, viewer))
  }

  /** The viewer has seen these grunts. Repeat views, and views of your own, don't count. */
  def recordViews(viewer: User, ids: Seq[String]): Future[Unit] =
    if (ids.isEmpty) Future.unit
    else grunts.recordViews(ids.distinct.take(MaxViewBatch), viewer.id, Reader(viewer))

  private def page(entries: Future[Seq[Grunt]], viewer: Option[User]): Future[FeedPage] =
    entries.flatMap { list =>
      // The next page starts below the last entry fetched, whether or not it was shown.
      present(list, viewer).map(FeedPage(_, Option.when(list.size == PageSize)(list.last.createdAt)))
    }

  /** A grunt or reply that's there and that the viewer may see: a reply, if they may see the grunt it answers. */
  private def findVisible(id: String, viewer: Option[User]): Future[Option[Grunt]] = {
    val reader = Reader.of(viewer)
    def visible(g: Grunt): Future[Boolean] = g.replyTo match {
      case None => Future.successful(!g.friendsOnly || reader.friendOf.contains(g.authorId))
      case Some(parent) => grunts.findById(parent).flatMap(_.fold(Future.successful(false))(visible))
    }
    grunts.findById(id).flatMap {
      case Some(g) if g.regruntOf.isEmpty => visible(g).map(Option.when(_)(g))
      case _ => Future.successful(None)
    }
  }

  /**
   * Feed entries as they're shown. A regrunt becomes the grunt it regrunted, marked with
   * who regrunted it; one the viewer can't see is dropped, and so is a grunt already shown
   * higher up (posted and regrunted, or regrunted by two people you follow).
   */
  private def present(entries: Seq[Grunt], viewer: Option[User]): Future[Seq[GruntView]] =
    for {
      originals <- grunts.findByIds(entries.flatMap(_.regruntOf).distinct, Reader.of(viewer)).map(_.map(g => g.id -> g).toMap)
      shown = entries.flatMap { e =>
        e.regruntOf match {
          case None => Some((e, None, e.createdAt))
          case Some(id) => originals.get(id).map(g => (g, Some(e.authorId), e.createdAt))
        }
      }.distinctBy(_._1.id)
      people <- users.findByIds(shown.flatMap((g, by, _) => g.authorId +: by.toSeq).distinct).map(_.map(u => u.id -> u).toMap)
      mine <- viewer.fold(Future.successful(Set.empty[String]))(v => grunts.regruntedBy(v.id, shown.map(_._1.id)))
    } yield shown.collect {
      case (g, by, at) if people.contains(g.authorId) =>
        GruntView(
          g.id, AuthorView.from(people(g.authorId)), MessageRenderer.toHtml(g.message), g.createdAt, g.friendsOnly, g.views,
          g.replyTo, g.replyCount, g.regruntCount, mine.contains(g.id), by.flatMap(people.get).map(AuthorView.from), at
        )
    }
}

object GruntService {
  enum Deletion { case Deleted, NotFound, NotYours }

  /** Why a change to a grunt was refused. */
  enum Problem {
    case NotFound
    case NotAllowed(message: String)
    case Invalid(message: String)
  }

  val MaxLength = 1000
  val PageSize = 50
  val MaxReplies = 500
  val MaxViewBatch = 100

  val HotWindowMillis: Long = 7L * 24 * 60 * 60 * 1000
  /** The most recent noticed grunts that are ranked; plenty for a small group's week. */
  val HotCandidates = 1000

  /**
   * (views + 2 × replies + 3 × regrunts) / (hours old + 2)^1.5. Answering or passing a
   * grunt on takes more than glancing at it, so they count for more than a view.
   */
  def hotScore(g: Grunt, now: Long): Double = {
    val hours = math.max(0L, now - g.createdAt) / 3600000.0
    (g.views + 2.0 * g.replyCount + 3.0 * g.regruntCount) / math.pow(hours + 2, 1.5)
  }
}
