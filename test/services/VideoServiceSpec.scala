package services

import org.scalatestplus.play.PlaySpec

class VideoServiceSpec extends PlaySpec {
  private def bytes(s: String*) = s.mkString.getBytes("ISO-8859-1")

  "typeForBytes" should {
    "recognise MP4 and iPhone QuickTime by their ftyp box" in {
      VideoService.typeForBytes(bytes("\u0000\u0000\u0000\u0018", "ftyp", "isom", "\u0000\u0000\u0002\u0000")) mustBe Some("video/mp4")
      VideoService.typeForBytes(bytes("\u0000\u0000\u0000\u0014", "ftyp", "qt  ", "\u0000\u0000\u0000\u0000")) mustBe Some("video/mp4")
    }
    "recognise WebM" in {
      VideoService.typeForBytes(bytes("\u001A\u0045\u00DF\u00A3", "\u0001\u0000\u0000\u0000")) mustBe Some("video/webm")
    }
    "refuse anything else" in {
      VideoService.typeForBytes(bytes("<html><script>alert(1)</script>")) mustBe None
      VideoService.typeForBytes(Array.emptyByteArray) mustBe None
    }
  }
}
