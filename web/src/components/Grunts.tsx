import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router";
import { api, gruntPath, imageUrl, videoUrl, type Grunt } from "../api.ts";
import { useSession } from "../session.tsx";
import { watchForView } from "../views.ts";
import { DotsIcon, RegruntIcon, ReplyIcon, ShareIcon, Switch } from "./Icons.tsx";

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
  /** Called with a grunt as it now stands, after a regrunt, a reply or a change. */
  onChanged: (grunt: Grunt) => void;
  /** Called after one of your own grunts is deleted. */
  onDeleted: (grunt: Grunt) => void;
};

export function GruntList({ grunts, more, onMore, onChanged, onDeleted }: ListProps) {
  return (
    <div className="grunts">
      {grunts.map((g) => (
        <GruntItem key={`${g.id}@${g.feedAt}`} grunt={g} onChanged={onChanged} onDeleted={() => onDeleted(g)} />
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

type ItemProps = {
  grunt: Grunt;
  onChanged: (grunt: Grunt) => void;
  onDeleted: () => void;
  /** Shown on its own page: bigger, not a link to itself, and Reply goes to the page's reply box. */
  focused?: boolean;
  onReply?: () => void;
};

/**
 * A grunt in a list. Clicking it (anywhere that isn't a link or a control) opens its page.
 * Reply opens a box under it; your replies from there show under it too.
 */
export function GruntItem({ grunt, onChanged, onDeleted, focused = false, onReply }: ItemProps) {
  const { author, regruntedBy } = grunt;
  const { user } = useSession();
  const navigate = useNavigate();
  const [replying, setReplying] = useState(false);
  const [myReplies, setMyReplies] = useState<Grunt[]>([]);
  const mine = user?.id === author.id;
  const element = useRef<HTMLElement>(null);

  // Signed-in readers' views count; your own don't.
  const watch = user !== null && !mine;
  useEffect(() => (watch && element.current ? watchForView(element.current, grunt.id) : undefined), [watch, grunt.id]);

  function open(e: React.MouseEvent) {
    if (focused) return;
    const target = e.target as HTMLElement;
    if (target.closest("a, button, video, input, textarea, [role=menu], .grunt-replies")) return;
    if (window.getSelection()?.toString()) return; // selecting text, not clicking
    navigate(gruntPath(grunt));
  }

  return (
    <article className={`grunt${focused ? " focused" : ""}`} ref={element} onClick={open}>
      {regruntedBy && (
        <div className="regrunted-by">
          <RegruntIcon />
          <Link to={`/${regruntedBy.handle}`}>{regruntedBy.id === user?.id ? "You" : regruntedBy.fullName}</Link> regrunted
        </div>
      )}
      <div className="grunt-row">
        <img className="avatar" src={imageUrl(author.profileImage, true)} alt="" />
        <div className="grunt-body">
          <GruntMeta grunt={grunt} onChanged={onChanged} onDeleted={onDeleted} />
          {/* Rendered from Markdown and cleaned on the server (MessageRenderer). */}
          <div className="grunt-message" dangerouslySetInnerHTML={{ __html: grunt.html }} />
          <GruntActions grunt={grunt} onChanged={onChanged} onReply={onReply ?? (() => setReplying((r) => !r))} />
        </div>
      </div>
      {(replying || myReplies.length > 0) && (
        <div className="grunt-replies">
          {myReplies.map((r) => (
            <ReplyItem key={r.id} reply={r} onDeleted={() => setMyReplies((list) => list.filter((x) => x.id !== r.id))} />
          ))}
          {myReplies.length > 0 && (
            <Link to={gruntPath(grunt)} className="see-conversation">
              See the whole conversation
            </Link>
          )}
          {replying && (
            <Composer
              replyTo={grunt}
              autoFocus
              onPosted={(r) => {
                setMyReplies((list) => [...list, r]);
                setReplying(false);
                onChanged({ ...grunt, replyCount: grunt.replyCount + 1 });
              }}
            />
          )}
        </div>
      )}
    </article>
  );
}

/** A reply, under its grunt. */
export function ReplyItem({ reply, onDeleted, highlight = false }: { reply: Grunt; onDeleted: () => void; highlight?: boolean }) {
  return (
    <article className={`reply${highlight ? " highlight" : ""}`} id={`reply-${reply.id}`}>
      <div className="grunt-row">
        <img className="avatar small" src={imageUrl(reply.author.profileImage, true)} alt="" />
        <div className="grunt-body">
          <GruntMeta grunt={reply} onChanged={() => {}} onDeleted={onDeleted} />
          <div className="grunt-message" dangerouslySetInnerHTML={{ __html: reply.html }} />
        </div>
      </div>
    </article>
  );
}

/** Name and handle; on the right, the time (always) and the ⋯ menu (on hover). */
function GruntMeta({ grunt, onChanged, onDeleted }: { grunt: Grunt; onChanged: (g: Grunt) => void; onDeleted: () => void }) {
  const { author } = grunt;
  return (
    <div className="grunt-meta">
      <Link to={`/${author.handle}`} className="name">
        {author.fullName}
      </Link>
      <span className="handle">@{author.handle}</span>
      <span className="grunt-corner">
        <Link to={gruntPath(grunt)} className="grunt-time">
          <time dateTime={new Date(grunt.createdAt).toISOString()} title={new Date(grunt.createdAt).toLocaleString()}>
            {formatTime(grunt.createdAt)}
          </time>
        </Link>
        <GruntMenu grunt={grunt} onChanged={onChanged} onDeleted={onDeleted} />
      </span>
    </div>
  );
}

/** Reply, regrunt and share; views and who can see it on the right. */
function GruntActions({ grunt, onChanged, onReply }: { grunt: Grunt; onChanged: (g: Grunt) => void; onReply: () => void }) {
  const { user } = useSession();
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  const [shared, setShared] = useState<string | null>(null);
  const mine = user?.id === grunt.author.id;

  const regruntBlocked = !user
    ? "Log in to regrunt"
    : mine
      ? "This is your own grunt"
      : grunt.friendsOnly
        ? "Friends-only grunts can't be regrunted"
        : null;

  async function toggleRegrunt() {
    setBusy(true);
    try {
      onChanged(await (grunt.regruntedByMe ? api.unregrunt(grunt.id) : api.regrunt(grunt.id)));
    } catch (e) {
      window.alert(e instanceof Error ? e.message : "Could not regrunt that.");
    } finally {
      setBusy(false);
    }
  }

  async function share() {
    const result = await shareGrunt(grunt);
    if (result) {
      setShared(result);
      window.setTimeout(() => setShared(null), 2000);
    }
  }

  return (
    <div className="grunt-actions">
      <button
        type="button"
        className="action"
        title="Reply"
        onClick={() => (user ? onReply() : navigate(gruntPath(grunt)))}
      >
        <ReplyIcon />
        <span>{grunt.replyCount > 0 ? grunt.replyCount : "Reply"}</span>
      </button>
      <button
        type="button"
        className={`action regrunt${grunt.regruntedByMe ? " on" : ""}`}
        title={regruntBlocked ?? (grunt.regruntedByMe ? "Undo regrunt" : "Regrunt to your followers")}
        aria-pressed={grunt.regruntedByMe}
        disabled={busy || regruntBlocked !== null}
        onClick={() => void toggleRegrunt()}
      >
        <RegruntIcon />
        <span>{grunt.regruntCount > 0 ? grunt.regruntCount : "Regrunt"}</span>
      </button>
      <button type="button" className="action" title="Share a link to this grunt" onClick={() => void share()}>
        <ShareIcon />
        <span aria-live="polite">{shared ?? "Share"}</span>
      </button>
      <span className="grunt-stats">
        {grunt.friendsOnly && (
          <span className="grunt-friends" title={`Only @${grunt.author.handle} and the people they follow can see this`}>
            Friends only
          </span>
        )}
        <span title="People who have seen it">
          {grunt.views} {grunt.views === 1 ? "view" : "views"}
        </span>
      </span>
    </div>
  );
}

/** On a phone, the system share sheet; elsewhere, the link copied. */
async function shareGrunt(grunt: Grunt): Promise<string | null> {
  const url = new URL(gruntPath(grunt), window.location.origin).href;
  if (navigator.share && window.matchMedia("(pointer: coarse)").matches) {
    try {
      await navigator.share({ url, title: `@${grunt.author.handle} on OpenWalrus` });
      return "Shared";
    } catch {
      return null; // closed without sharing
    }
  }
  try {
    await navigator.clipboard.writeText(url);
    return "Link copied";
  } catch {
    window.prompt("Copy this link:", url); // no clipboard over plain http
    return null;
  }
}

/** The ⋯ menu: who can see it (the author's switch), the link, and Delete for the author. */
function GruntMenu({ grunt, onChanged, onDeleted }: { grunt: Grunt; onChanged: (g: Grunt) => void; onDeleted: () => void }) {
  const { user } = useSession();
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [copied, setCopied] = useState(false);
  const box = useRef<HTMLDivElement>(null);
  const mine = user?.id === grunt.author.id;

  useEffect(() => {
    if (!open) return;
    const outside = (e: PointerEvent) => {
      if (!box.current?.contains(e.target as Node)) setOpen(false);
    };
    const escape = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    document.addEventListener("pointerdown", outside);
    document.addEventListener("keydown", escape);
    return () => {
      document.removeEventListener("pointerdown", outside);
      document.removeEventListener("keydown", escape);
    };
  }, [open]);

  async function toggleFriendsOnly() {
    const next = !grunt.friendsOnly;
    if (next && grunt.regruntCount > 0) {
      const n = grunt.regruntCount;
      if (!window.confirm(`Making it friends-only also undoes its ${n} ${n === 1 ? "regrunt" : "regrunts"}.`)) return;
    }
    setBusy(true);
    try {
      onChanged(await api.setFriendsOnly(grunt.id, next));
    } catch (e) {
      window.alert(e instanceof Error ? e.message : "Could not change that.");
    } finally {
      setBusy(false);
    }
  }

  async function copyLink() {
    try {
      await navigator.clipboard.writeText(new URL(gruntPath(grunt), window.location.origin).href);
      setCopied(true);
      window.setTimeout(() => {
        setCopied(false);
        setOpen(false);
      }, 900);
    } catch {
      window.prompt("Copy this link:", new URL(gruntPath(grunt), window.location.origin).href);
    }
  }

  async function remove() {
    setOpen(false);
    if (!window.confirm(grunt.replyTo ? "Delete this reply?" : "Delete this grunt, and its replies?")) return;
    try {
      await api.deleteGrunt(grunt.id);
      onDeleted();
    } catch (e) {
      window.alert(e instanceof Error ? e.message : "Could not delete that.");
    }
  }

  return (
    <div className="grunt-menu" ref={box}>
      <button
        type="button"
        className="dots"
        aria-label="More"
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((o) => !o)}
      >
        <DotsIcon />
      </button>
      {open && (
        <div className="menu" role="menu">
          {mine && !grunt.replyTo && (
            <button
              type="button"
              role="menuitemcheckbox"
              aria-checked={grunt.friendsOnly}
              disabled={busy}
              title="Only you and the people you follow see it"
              onClick={() => void toggleFriendsOnly()}
            >
              Friends only
              <Switch on={grunt.friendsOnly} />
            </button>
          )}
          <button type="button" role="menuitem" onClick={() => void copyLink()}>
            {copied ? "Link copied" : "Copy link"}
          </button>
          {mine && (
            <button type="button" role="menuitem" className="danger" onClick={() => void remove()}>
              Delete
            </button>
          )}
        </div>
      )}
    </div>
  );
}

function formatTime(ms: number): string {
  const seconds = Math.round((Date.now() - ms) / 1000);
  if (seconds < 60) return "just now";
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}h`;
  return new Date(ms).toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
}

type ComposerProps = {
  prefill?: string;
  /** Writing a reply to this grunt, rather than a grunt of your own. */
  replyTo?: Grunt;
  autoFocus?: boolean;
  onPosted: (grunt: Grunt) => void;
};

/**
 * Where a signed-in user writes a grunt or a reply. Markdown works. Pictures and videos
 * are uploaded and linked in: chosen with the button, dropped onto the box, or pasted.
 */
export function Composer({ prefill = "", replyTo, autoFocus = false, onPosted }: ComposerProps) {
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
      onPosted(await (replyTo ? api.reply(replyTo.id, text) : api.postGrunt(text, friendsOnly)));
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
      className={`composer card${replyTo ? " compact" : ""}${dropping ? " dropping" : ""}`}
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
        id={replyTo ? `reply-to-${replyTo.id}` : undefined}
        aria-label={replyTo ? `Your reply to @${replyTo.author.handle}` : "Your grunt"}
        placeholder={
          replyTo ? `Reply to @${replyTo.author.handle}…` : "What's up with you? Drop or paste in pictures and videos too."
        }
        autoFocus={autoFocus}
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
        {!replyTo && (
          <button
            type="button"
            role="switch"
            aria-checked={friendsOnly}
            className="switch-button"
            title="Only you and the people you follow will see it"
            onClick={() => setFriendsOnly((f) => !f)}
          >
            <Switch on={friendsOnly} />
            Friends only
          </button>
        )}
        <button type="button" className="secondary" disabled={busy} onClick={() => fileInput.current?.click()}>
          Add photo or video
        </button>
        <button type="submit" disabled={busy || !text.trim()}>
          {replyTo ? "Reply" : "Grunt"}
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
