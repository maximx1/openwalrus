package core.utils

import core.BaseTestSpec

class MessageUtilsTest extends BaseTestSpec {
  "A message" should {
    "pull out one user if one user is referenced at the beginning" in {
      val results = MessageUtils.pullAtUsers("@justin is the coolest")
      results should have length 1
      results should contain("justin")
    }

    "pull out one user if one user is referenced at the end" in {
      val results = MessageUtils.pullAtUsers("The coolest is @al")
      results should have length 1
      results should contain("al")
    }

    "pull out one user if one user is referenced in the middle" in {
      val results = MessageUtils.pullAtUsers("Who is the coolest? @n is the coolest")
      results should have length 1
      results should contain("n")
    }

    "pull out one user if they are the entire message" in {
      val results = MessageUtils.pullAtUsers("@BadMamboJambo")
      results should have length 1
      results should contain("BadMamboJambo")
    }

    "pull out multiple users if one is at the beginning, one at the end, and one in the middle" in {
      val results = MessageUtils.pullAtUsers("@PatBenatar is definitely classic, And you can't beat @JoanJett, but I also like @Nena")
      results should have length 3
      results should contain("PatBenatar")
      results should contain("JoanJett")
      results should contain("Nena")
    }

    "pull out multiple users one right after the other" in {
      val results = MessageUtils.pullAtUsers("Love to party with my boys: @Sam @Toby")
      results should have length 2
      results should contain("Sam")
      results should contain("Toby")
    }

    "pull out multiple users one right after the other if they are the only ones in the message" in {
      val results = MessageUtils.pullAtUsers("@Pete @Townsend")
      results should have length 2
      results should contain("Pete")
      results should contain("Townsend")
    }
  }
  
  "HTML" should {
    "be able to be stripped and turned into plain text with line breaks" in {
      val original = "### My recipe<br><pre style=\"color: rgb(0, 0, 0); line-height: normal;\">![BBQ Pulled Pork](http://i.imgur.com/YhItOoy.jpg \"IBC BBQ Pulled Pork\")</pre>"
      val expected = "### My recipe\n![BBQ Pulled Pork](http://i.imgur.com/YhItOoy.jpg \"IBC BBQ Pulled Pork\")"
      val actual = MessageUtils.htmlToTextWithNewLines(original)
      actual shouldBe expected
    }
  }
}
