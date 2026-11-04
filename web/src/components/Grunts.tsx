import { useEffect, useRef, useState } from "react";
import { Link } from "react-router";
import { api, imageUrl, videoUrl, type Grunt } from "../api.ts";
import { useSession } from "../session.tsx";
import { watchForView } from "../views.ts";

const MAX_LENGTH = 1000;
const MB = 1024 * 1024;

// What the server accepts (ImageService, VideoService). Checked here too, so a wrong or
// oversized file is turned away before it's sent rather than after.
const IMAGE_TYPES = ["image/png", "image/jpeg", "image/gif"];
const VIDEO_TYPES = ["video/mp4", "video/quicktime", "video/webm"];
const ACCEPT = [...IMAGE_TYPES, ...VIDEO_TYPES].join(",");

function kindOf(file: File): "image" | "video" | null {
  if (IMAGE_TYPES.includes(file.type)) return "image";
  if (VIDEO_TYPES.includes(file.type) || /\.(mp4|m4v|mov|webm)$/i.test(file.name)) return "video";
  return null;
}

const hasFiles = (e: React.DragEvent | DragEvent) => e.dataTransfer?.types.includes("Files") ?? false;

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

/**
 * Where a signed-in user writes a grunt. Markdown works. Pictures and videos are uploaded
 * and linked in: chosen with the button, dropped onto the box, or pasted.
 */
export function Composer({ prefill = "", onPosted }: { prefill?: string; onPosted: (grunt: Grunt) => void }) {
  const [text, setText] = useState(prefill);
  const [friendsOnly, setFriendsOnly] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [uploading, setUploading] = useState<{ name: string; fraction: number } | null>(null);
  const [dropping, setDropping] = useState(false);
  const dragDepth = useRef(0); // dragenter/leave fire for every child the pointer crosses
  const fileInput = useRef<HTMLInputElement>(null);

  // A file dropped just beside the box would otherwise replace the page with it.
  useEffect(() => {
    const guard = (e: DragEvent) => {
      if (hasFiles(e)) e.preventDefault();
    };
    window.addEventListener("dragover", guard);
    window.addEventListener("drop", guard);
    return () => {
      window.removeEventListener("dragover", guard);
      window.removeEventListener("drop", guard);
    };
  }, []);

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

  /** Uploads each in turn and adds it to the end of the grunt. */
  async function addFiles(files: File[]) {
    if (files.length === 0 || busy) return;
    setBusy(true);
    setError(null);
    try {
      for (const file of files) {
        const kind = kindOf(file);
        if (!kind) throw new Error(`${file.name}: only pictures (PNG, JPEG, GIF) and videos (MP4, MOV, WebM) can be added.`);
        if (kind === "image" && file.size > 10 * MB) throw new Error(`${file.name}: pictures can be 10 MB at most.`);
        if (kind === "video" && file.size > 50 * MB) throw new Error(`${file.name}: videos can be 50 MB at most.`);
        setUploading({ name: file.name, fraction: 0 });
        const progress = (fraction: number) => setUploading({ name: file.name, fraction });
        const markdown =
          kind === "image"
            ? `![image](${imageUrl((await api.uploadImage(file, progress)).id)})`
            : `![video](${videoUrl((await api.uploadVideo(file, progress)).id)})`;
        setText((t) => `${t}${t && !t.endsWith("\n") ? "\n" : ""}${markdown}\n`);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not upload that.");
    } finally {
      setUploading(null);
      setBusy(false);
    }
  }

  const status = uploading
    ? `Uploading ${uploading.name}… ${Math.round(uploading.fraction * 100)}%`
    : text.length > MAX_LENGTH - 100
      ? `${MAX_LENGTH - text.length} left`
      : "Markdown works";

  return (
    <form
      className={`composer card${dropping ? " dropping" : ""}`}
      onSubmit={(e) => {
        e.preventDefault();
        void post();
      }}
      onDragEnter={(e) => {
        if (!hasFiles(e)) return;
        dragDepth.current += 1;
        setDropping(true);
      }}
      onDragOver={(e) => {
        if (!hasFiles(e)) return;
        e.preventDefault();
        e.dataTransfer.dropEffect = "copy";
      }}
      onDragLeave={(e) => {
        if (!hasFiles(e)) return;
        dragDepth.current -= 1;
        if (dragDepth.current <= 0) setDropping(false);
      }}
      onDrop={(e) => {
        if (!hasFiles(e)) return;
        e.preventDefault();
        dragDepth.current = 0;
        setDropping(false);
        void addFiles([...e.dataTransfer.files]);
      }}
    >
      <textarea
        aria-label="Your grunt"
        placeholder="What's up with you? Drop or paste in pictures and videos too."
        value={text}
        maxLength={MAX_LENGTH}
        onChange={(e) => setText(e.target.value)}
        onPaste={(e) => {
          const files = [...e.clipboardData.files];
          if (files.length === 0) return; // ordinary text: let it paste
          e.preventDefault();
          void addFiles(files);
        }}
        onKeyDown={(e) => {
          if (e.key === "Enter" && (e.ctrlKey || e.metaKey)) void post();
        }}
      />
      {error && <div className="error">{error}</div>}
      <div className="composer-actions">
        <span className="hint" aria-live="polite">
          {status}
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
          Add photo or video
        </button>
        <button type="submit" disabled={busy || !text.trim()}>
          Grunt
        </button>
        <input
          ref={fileInput}
          type="file"
          accept={ACCEPT}
          multiple
          hidden
          onChange={(e) => {
            void addFiles([...(e.target.files ?? [])]);
            e.target.value = "";
          }}
        />
      </div>
    </form>
  );
}
