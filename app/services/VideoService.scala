package services

import javax.inject.{Inject, Singleton}

import repositories.{StoredVideo, VideoRepository}

import java.io.FileInputStream
import java.nio.file.Path
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Using

/**
 * Accepts uploaded videos and stores them as they are: nothing is converted, so a video
 * plays where its format does. MP4 (H.264) plays in every browser; an iPhone's HEVC .mov
 * only in Safari.
 */
@Singleton
class VideoService @Inject() (videos: VideoRepository)(using ec: ExecutionContext) {
  import VideoService.*

  /** Left is a message for the person uploading. */
  def store(file: Path, fileName: String): Future[Either[String, String]] =
    typeForBytes(Using.resource(new FileInputStream(file.toFile))(_.readNBytes(16))) match {
      case None => Future.successful(Left("That doesn't look like an MP4, MOV or WebM video."))
      case Some(contentType) => videos.store(file, contentType, fileName).map(Right(_))
    }

  def open(id: String): Future[Option[StoredVideo]] = videos.open(id)
}

object VideoService {
  val MaxBytes: Long = 50L * 1024 * 1024

  /**
   * Decides by the file's first bytes. MP4 and QuickTime both start with an "ftyp" box;
   * both are served as video/mp4, which browsers that can decode a .mov will then play.
   */
  def typeForBytes(b: Array[Byte]): Option[String] =
    if (b.length >= 12 && new String(b.slice(4, 8), "US-ASCII") == "ftyp") Some("video/mp4")
    else if (b.length >= 4 && b(0) == 0x1A.toByte && b(1) == 0x45.toByte && b(2) == 0xDF.toByte && b(3) == 0xA3.toByte) Some("video/webm")
    else None
}
