package walrath.technology.openwalrus.model.tos

import org.bson.types.ObjectId

case class UserTO(
  id: Option[ObjectId],
  handle: String,
  email: Option[String],
  phone: Option[String],
  password: String,
  fullName: String,
  creationDate: Long,
  currentlyActivated: Boolean,
  verified: Boolean,
  profileImage: String,
  images: List[ObjectId],
  grunts: List[ObjectId],
  following: List[ObjectId],
  followers: List[ObjectId]
) {
  def u = this
  def toUser = User(u.id, u.handle, u.email, u.phone, u.password, u.fullName, u.creationDate, u.currentlyActivated, u.verified,
    convertToId(u.profileImage), u.images, u.grunts, u.following, u.followers)
  def convertToId(str: String) = if(ObjectId.isValid(str)) Some(new ObjectId(str)) else None
}

object UserTO {
  def fromUser(u: User) =
    UserTO(u.id, u.handle, u.email, u.phone, u.password, u.fullName, u.creationDate, u.currentlyActivated, u.verified,
      u.profileImage.map(_.toString).getOrElse("noimage"), u.images, u.grunts, u.following, u.followers)
}