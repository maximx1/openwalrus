import { useEffect, useRef, useState } from "react";
import { Link } from "react-router";
import { api, imageUrl, type Grunt } from "../api.ts";
import { useSession } from "../session.tsx";
import { watchForView } from "../views.ts";

const MAX_LENGTH = 1000;

type ListProps = {
  grunts: Grunt[];
  more: boolean;
  onMore: () => void;
  /** Called after one of your own grunts is deleted. */
  onDeleted: (grunt: Grunt) => void;
};

export function GruntList({ grunts, more, onMore, onDeleted }: ListProps) {
  return (
    <div className="grunts">
      {grunts.map((g) => (
        <GruntItem key={g.id} grunt={g} onDeleted={() => onDeleted(g)} />
      ))}
      {more ? (
        <button type="button" className="end-of-grunts" onClick={onMore}>
          Older grunts…
        </button>
      ) : (
        <div className="end-of-grunts">End of grunts…</div>
      )}
    </div>
  );
}

function GruntItem({ grunt, onDeleted }: { grunt: Grunt; onDeleted: () => void }) {
  const { author } = grunt;
  const { user } = useSession();
  const [busy, setBusy] = useState(false);
  const mine = user?.id === author.id;
  const element = useRef<HTMLElement>(null);

  // Signed-in readers' views count; your own don't.
  const watch = user !== null && !mine;
  useEffect(() => (watch && element.current ? watchForView(element.current, grunt.id) : undefined), [watch, grunt.id]);

  async function remove() {
    if (!window.confirm("Delete this grunt?")) return;
    setBusy(true);
    try {
      await api.deleteGrunt(grunt.id);
      onDeleted();
    } catch (e) {
      window.alert(e instanceof Error ? e.message : "Could not delete that.");
      setBusy(false);
    }
  }

  return (
    <article className="grunt" ref={element}>
      <img className="avatar" src={imageUrl(author.profileImage, true)} alt="" />
      <div className="grunt-body">
        <div className="grunt-meta">
          <Link to={`/${author.handle}`} className="name">
            {author.fullName}
          </Link>{" "}
          <span className="handle">@{author.handle}</span>
          <time dateTime={new Date(grunt.createdAt).toISOString()}>{formatTime(grunt.createdAt)}</time>
        </div>
        {/* Rendered from Markdown and cleaned on the server (MessageRenderer). */}
        <div className="grunt-message" dangerouslySetInnerHTML={{ __html: grunt.html }} />
        <div className="grunt-foot">
          <span className="grunt-views" title="People who have seen it">
            {grunt.views} {grunt.views === 1 ? "view" : "views"}
          </span>
          {grunt.friendsOnly && (
            <span className="grunt-friends" title={`Only @${author.handle} and the people they follow can see this`}>
              Friends only
            </span>
          )}
          {mine && (
            <button type="button" className="grunt-delete" disabled={busy} onClick={() => void remove()}>
              Delete
            </button>
          )}
        </div>
      </div>
    </article>
  );
}

function formatTime(ms: number): string {
  const seconds = Math.round((Date.now() - ms) / 1000);
  if (seconds < 60) return "just now";
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}h`;
  return new Date(ms).toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
}

/** Where a signed-in user writes a grunt. Markdown works; images are uploaded and linked in. */
export function Composer({ prefill = "", onPosted }: { prefill?: string; onPosted: (grunt: Grunt) => void }) {
  const [text, setText] = useState(prefill);
  const [friendsOnly, setFriendsOnly] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const fileInput = useRef<HTMLInputElement>(null);

  async function post() {
    if (!text.trim()) return;
    setBusy(true);
    setError(null);
    try {
      onPosted(await api.postGrunt(text, friendsOnly));
      setText(prefill);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not post that.");
    } finally {
      setBusy(false);
    }
  }

  async function addImage(file: File | undefined) {
    if (!file) return;
    setBusy(true);
    setError(null);
    try {
      const { id } = await api.uploadImage(file);
      setText((t) => `${t}${t && !t.endsWith("\n") ? "\n" : ""}![image](${imageUrl(id)})\n`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not upload that.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <form
      className="composer card"
      onSubmit={(e) => {
        e.preventDefault();
        void post();
      }}
    >
      <textarea
        aria-label="Your grunt"
        placeholder="What's up with you?"
        value={text}
        maxLength={MAX_LENGTH}
        onChange={(e) => setText(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter" && (e.ctrlKey || e.metaKey)) void post();
        }}
      />
      {error && <div className="error">{error}</div>}
      <div className="composer-actions">
        <span className="hint">
          {text.length > MAX_LENGTH - 100 ? `${MAX_LENGTH - text.length} left` : "Markdown works"}
        </span>
        <button
          type="button"
          className={`secondary friends-toggle${friendsOnly ? " on" : ""}`}
          aria-pressed={friendsOnly}
          title="Only you and the people you follow will see it"
          onClick={() => setFriendsOnly((f) => !f)}
        >
          Friends only
        </button>
        <button type="button" className="secondary" disabled={busy} onClick={() => fileInput.current?.click()}>
          Add image
        </button>
        <button type="submit" disabled={busy || !text.trim()}>
          Grunt
        </button>
        <input
          ref={fileInput}
          type="file"
          accept="image/png,image/jpeg,image/gif"
          hidden
          onChange={(e) => {
            void addImage(e.target.files?.[0]);
            e.target.value = "";
          }}
        />
      </div>
    </form>
  );
}
