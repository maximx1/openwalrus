package services

import org.scalatestplus.play.PlaySpec

class MessageRendererSpec extends PlaySpec {

  "mentionedHandles" should {
    "find each @handle once, ignoring case and email addresses" in {
      MessageRenderer.mentionedHandles("hi @bob and @Bob, mail me at me@example.com, @sue_2") mustBe Seq("bob", "sue_2")
    }
  }

  "toHtml" should {
    "render Markdown" in {
      MessageRenderer.toHtml("**big** news") must include("<strong>big</strong>")
    }

    "escape HTML typed into the message" in {
      val html = MessageRenderer.toHtml("""<script>alert(1)</script><img src=x onerror=alert(1)>""")
      html must not include "<script"
      html must not include "<img"
    }

    "drop javascript: links" in {
      MessageRenderer.toHtml("[click](javascript:alert(1))") must not include "javascript:"
    }

    "keep images uploaded to the app" in {
      MessageRenderer.toHtml("![pic](/api/images/abc)") must include("""src="/api/images/abc"""")
    }

    "link @mentions to profiles, but not inside code" in {
      val html = MessageRenderer.toHtml("hey @bob\n\n`@notalink`")
      html must include("""<a href="/bob">@bob</a>""")
      html must include("<code>@notalink</code>")
    }
  }
}
