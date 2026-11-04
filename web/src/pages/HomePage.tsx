import { useCallback } from "react";
import { Link } from "react-router";
import { api } from "../api.ts";
import { Composer, GruntList } from "../components/Grunts.tsx";
import { ProfileCard } from "../components/ProfileCard.tsx";
import { useSession } from "../session.tsx";
import { usePagedGrunts } from "../usePagedGrunts.ts";
import { TeamPage } from "./TeamPage.tsx";

/** Signed in: your feed. Signed out: everyone, and a way in. */
export function HomePage() {
  const { user } = useSession();
  if (!user) return <Welcome />;
  return <Feed />;
}

function Feed() {
  const session = useSession();
  const me = session.user!;
  const load = useCallback((before?: number) => api.feed(before), []);
  const { grunts, more, error, loadMore, prepend, remove, replace } = usePagedGrunts(load);

  return (
    <div className="two-column">
      <aside>
        <ProfileCard user={me} onChange={session.setUser} />
      </aside>
      <section>
        <Composer
          onPosted={(g) => {
            prepend(g);
            session.setUser({ ...me, gruntCount: me.gruntCount + 1 });
          }}
        />
        {error && <div className="error">{error}</div>}
        {grunts.length === 0 && !error ? (
          <p className="empty">
            Your feed is your grunts, the people you follow, and anything that mentions you. Find people on the{" "}
            <Link to="/team">Team</Link> page.
          </p>
        ) : (
          <GruntList
            grunts={grunts}
            more={more}
            onMore={loadMore}
            onChanged={replace}
            onDeleted={(g) => {
              remove(g.id);
              void session.refresh(); // your grunt count
            }}
          />
        )}
      </section>
    </div>
  );
}

function Welcome() {
  const { allowRegistration } = useSession();
  return (
    <>
      <div className="welcome">
        <img src="/walrus.svg" alt="" className="welcome-logo" />
        <p>A small place for your group to grunt at each other.</p>
        <div className="welcome-actions">
          {allowRegistration && (
            <Link to="/signup" className="button">
              Sign up
            </Link>
          )}
          <Link to="/login" className="button secondary">
            Log in
          </Link>
        </div>
      </div>
      <TeamPage />
    </>
  );
}
