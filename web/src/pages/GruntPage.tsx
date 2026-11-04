import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router";
import { api, ApiError, gruntPath, type Grunt } from "../api.ts";
import { Composer, GruntItem, ReplyItem } from "../components/Grunts.tsx";
import { useSession } from "../session.tsx";

/** A grunt on its own page, with its replies under it. (The handle in the address is for people; the id finds it.) */
export function GruntPage() {
  const { id = "" } = useParams();
  const { hash } = useLocation();
  const navigate = useNavigate();
  const session = useSession();
  const [grunt, setGrunt] = useState<Grunt | null>(null);
  const [replies, setReplies] = useState<Grunt[] | null>(null);
  const [missing, setMissing] = useState<string | null>(null);

  useEffect(() => {
    let live = true;
    setGrunt(null);
    setReplies(null);
    setMissing(null);
    (async () => {
      try {
        const g = await api.grunt(id);
        if (g.replyTo) {
          // A reply's link: show the conversation, scrolled to the reply.
          const parent = await api.grunt(g.replyTo);
          if (live) navigate(`${gruntPath(parent)}#reply-${g.id}`, { replace: true });
          return;
        }
        const list = await api.replies(id);
        if (!live) return;
        setGrunt(g);
        setReplies(list);
      } catch (e) {
        if (live) setMissing(e instanceof ApiError && e.status === 404 ? e.message : "Could not load that grunt.");
      }
    })();
    return () => {
      live = false;
    };
  }, [id, session.user?.id, navigate]);

  // Linked to a reply: bring it into view once it's there.
  useEffect(() => {
    if (replies && hash) document.getElementById(hash.slice(1))?.scrollIntoView({ block: "center" });
  }, [replies, hash]);

  if (missing)
    return (
      <div className="narrow">
        <h2 className="page-title">{missing}</h2>
        <p className="empty">
          <Link to="/">Back home</Link>
        </p>
      </div>
    );
  if (!grunt || !replies) return null;

  return (
    <div className="narrow grunt-page">
      <button type="button" className="link-button back" onClick={() => (window.history.length > 1 ? navigate(-1) : navigate("/"))}>
        ← Back
      </button>
      <div className="grunts">
        <GruntItem
          grunt={grunt}
          focused
          onChanged={setGrunt}
          onDeleted={() => navigate(`/${grunt.author.handle}`, { replace: true })}
          onReply={() => document.getElementById(`reply-to-${grunt.id}`)?.focus()}
        />
      </div>
      <h3 className="replies-title">
        {replies.length === 0 ? "No replies yet" : replies.length === 1 ? "1 reply" : `${replies.length} replies`}
      </h3>
      {session.user ? (
        <Composer
          replyTo={grunt}
          onPosted={(r) => {
            setReplies((list) => [...(list ?? []), r]);
            setGrunt((g) => g && { ...g, replyCount: g.replyCount + 1 });
          }}
        />
      ) : (
        <p className="hint">
          <Link to="/login">Log in</Link> to reply.
        </p>
      )}
      {replies.length > 0 && (
        <div className="grunts replies">
          {replies.map((r) => (
            <ReplyItem
              key={r.id}
              reply={r}
              highlight={hash === `#reply-${r.id}`}
              onDeleted={() => {
                setReplies((list) => list && list.filter((x) => x.id !== r.id));
                setGrunt((g) => g && { ...g, replyCount: g.replyCount - 1 });
              }}
            />
          ))}
        </div>
      )}
    </div>
  );
}
