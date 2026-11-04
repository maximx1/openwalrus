import com.google.inject.AbstractModule
import repositories.*
import repositories.mongo.*

/** Picks the database. Every repository the app uses is bound here and nowhere else. */
class Module extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[UserRepository]).to(classOf[MongoUserRepository])
    bind(classOf[GruntRepository]).to(classOf[MongoGruntRepository])
    bind(classOf[ImageRepository]).to(classOf[MongoImageRepository])
    bind(classOf[SessionRepository]).to(classOf[MongoSessionRepository])
    bind(classOf[DatabaseHealth]).to(classOf[MongoDatabaseProvider])
  }
}
