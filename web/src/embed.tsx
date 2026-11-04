import { StrictMode, useEffect, useLayoutEffect, useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import { api, gruntPath, imageUrl, type Grunt } from "./api.ts";
import { RegruntIcon, ReplyIcon } from "./components/Icons.tsx";
import { countViewsAsEmbeddedOn, watchForView } from "./views.ts";
import "./embed.css";

// The page at /embed/:id, which other sites put in an iframe: one grunt, and links that
// open OpenWalrus in a new tab. It's a separate bundle, so it stays small.

const id = location.pathname.match(/^\/embed\/([0-9a-f]{24})$/)?.[1] ?? new URLSearchParams(location.search).get("id") ?? "";

const framed = window.parent !== window;

/** The site it's embedded on, as far as the browser says. */
function parentSite(): string {
  try {
    const origin = location.ancestorOrigins?.[0] ?? document.referrer;
    return origin ? new URL(origin).host : "";
  } catch {
    return "";
  }
}

// Tell the page around it how tall it is (embed.js resizes the iframe to fit). Measured on
// the content, not the document, which is never shorter than the frame and so can't shrink.
const root = document.getElementById("root")!;
const reportHeight = () =>
  window.parent.postMessage({ type: "openwalrus:height", height: root.getBoundingClientRect().height }, "*");

// Shown in an iframe on our own pages (the Embed dialog's preview): not a view.
const site = parentSite();
const preview = framed && site === location.host;
if (framed) countViewsAsEmbeddedOn(site);

function EmbeddedGrunt() {
  const [grunt, setGrunt] = useState<Grunt | null>(null);
  const [missing, setMissing] = useState(false);
  const card = useRef<HTMLElement>(null);

  useEffect(() => {
    api.grunt(id).then(setGrunt, () => setMissing(true));
  }, []);

  useEffect(() => (grunt && !preview && card.current ? watchForView(card.current, grunt.id) : undefined), [grunt]);
  // Report the new height as soon as it's laid out, besides the ResizeObserver, which a
  // page that isn't on screen may hold back.
  useLayoutEffect(() => {
    if (framed) reportHeight();
  }, [grunt, missing]);

  if (missing) {
    return (
      <p className="embed-missing">
        This grunt isn't available. It may have been deleted, or shared with friends only.{" "}
        <a href="/" target="_blank" rel="noopener">
          OpenWalrus
        </a>
      </p>
    );
  }
  if (!grunt) return <div className="embed-loading" />;

  const { author } = grunt;
  const url = new URL(gruntPath(grunt), location.origin).href;
  const posted = new Date(grunt.createdAt);
  return (
    <article className="embed" ref={card}>
      <header>
        <a className="who" href={`/${author.handle}`} target="_blank" rel="noopener">
          <img src={imageUrl(author.profileImage, true)} alt="" />
          <span>
            <strong>{author.fullName}</strong>
            <span className="handle">@{author.handle}</span>
          </span>
        </a>
        <a className="brand" href="/" target="_blank" rel="noopener" title="OpenWalrus">
          <span className="brand-dot" />
        </a>
      </header>
      {/* Rendered from Markdown and cleaned on the server (MessageRenderer). */}
      <div className="message" dangerouslySetInnerHTML={{ __html: grunt.html }} />
      <footer>
        <a href={url} target="_blank" rel="noopener">
          <time dateTime={posted.toISOString()}>
            {posted.toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" })}
          </time>
        </a>
        <span className="counts">
          <span title="Replies">
            <ReplyIcon /> {grunt.replyCount}
          </span>
          <span title="Regrunts">
            <RegruntIcon /> {grunt.regruntCount}
          </span>
          <span>
            {grunt.views.toLocaleString()} {grunt.views === 1 ? "view" : "views"}
          </span>
        </span>
      </footer>
      <a className="open" href={url} target="_blank" rel="noopener">
        Reply on OpenWalrus
      </a>
    </article>
  );
}

// Links in the message (mentions, anything written) open in a new tab too, not in the frame.
document.addEventListener("click", (e) => {
  const link = (e.target as HTMLElement).closest("a");
  if (link) {
    link.target = "_blank";
    link.rel = "noopener";
  }
});

if (framed) {
  new ResizeObserver(reportHeight).observe(root);
  // embed.js asks again when it loads after the embed (it's async), having missed the first reports
  window.addEventListener("message", (e) => {
    if (e.source === window.parent && (e.data as { type?: string } | null)?.type === "openwalrus:measure") reportHeight();
  });
}

createRoot(root).render(
  <StrictMode>
    <EmbeddedGrunt />
  </StrictMode>,
);
