package services

import models.Grunt
import org.scalatestplus.play.PlaySpec

class HotScoreSpec extends PlaySpec {
  private val hour = 3600000L
  private val now = 1000 * hour
  private def grunt(hoursOld: Long, views: Int) = Grunt("g", "a", "", Seq.empty, now - hoursOld * hour, friendsOnly = false, views)
  private def score(g: Grunt) = GruntService.hotScore(g, now)

  "The hot score" should {
    "rank a newer grunt above an older one with the same views" in {
      score(grunt(1, 5)) must be > score(grunt(20, 5))
    }
    "let a few views on a new grunt beat many more on a days-old one" in {
      score(grunt(1, 4)) must be > score(grunt(72, 40))
    }
    "still rank more views first at the same age" in {
      score(grunt(10, 6)) must be > score(grunt(10, 5))
    }
    "be nothing without views" in {
      score(grunt(0, 0)) mustBe 0.0
    }
  }
}
