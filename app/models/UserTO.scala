package models

import org.bson.types.ObjectId

case class UserTO(
  id: String,
  handle: String,
  email: Option[String],
  phone: Option[String],
  password: String,
  fullName: String,
  creationDate: Long,
  currentlyActivated: Boolean,
  verified: Boolean,
  profileImage: String,
  bannerImage: String,
  images: List[ObjectId],
  grunts: List[ObjectId],
  following: List[String],
  followers: List[String]
) {
  def toUser = User(convertToId(id), handle, email, phone, password, fullName, creationDate, currentlyActivated, verified,
    convertToId(profileImage), convertToId(bannerImage), images, grunts, following.map(convertToId).filter(_ != None).map(_.get), followers.map(convertToId).filter(_ != None).map(_.get))
  private def convertToId(str: String) = if(ObjectId.isValid(str)) Some(new ObjectId(str)) else None
}

object UserTO {
  def fromUser(u: User) =
    UserTO(u.id.map(_.toString).getOrElse(""), u.handle, u.email, u.phone, u.password, u.fullName, u.creationDate, u.currentlyActivated, u.verified,
      u.profileImage.map(_.toString).getOrElse("noimage"), u.bannerImage.map(_.toString()).getOrElse("noimage"), u.images, u.grunts, u.following.map(_.toString), u.followers.map(_.toString))
}