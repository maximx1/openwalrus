import { useCallback, useEffect, useState } from "react";
import { NavLink, useParams } from "react-router";
import { api, ApiError, type User } from "../api.ts";
import { Composer, GruntList } from "../components/Grunts.tsx";
import { ProfileCard } from "../components/ProfileCard.tsx";
import { replaceUser, UserCards } from "../components/UserCards.tsx";
import { useSession } from "../session.tsx";
import { usePagedGrunts } from "../usePagedGrunts.ts";

type Tab = "grunts" | "following" | "followers";

export function ProfilePage({ tab }: { tab: Tab }) {
  const { handle = "" } = useParams();
  const session = useSession();
  const [user, setUser] = useState<User | null>(null);
  const [missing, setMissing] = useState(false);

  useEffect(() => {
    setUser(null);
    setMissing(false);
    api.user(handle).then(setUser, (e: unknown) => {
      if (e instanceof ApiError && e.status === 404) setMissing(true);
    });
  }, [handle, session.user?.id]);

  if (missing) return <h2 className="page-title">There is no @{handle} here.</h2>;
  if (!user) return null;

  const update = (next: User) => {
    if (next.id === user.id) setUser(next);
    if (next.id === session.user?.id) session.setUser(next);
  };

  return (
    <div className="two-column">
      <aside>
        <ProfileCard user={user} onChange={update} />
      </aside>
      <section>
        <div className="tabs">
          <NavLink to={`/${user.handle}`} end>
            Grunts
          </NavLink>
          <NavLink to={`/${user.handle}/following`}>Following</NavLink>
          <NavLink to={`/${user.handle}/followers`}>Followers</NavLink>
        </div>
        {tab === "grunts" ? (
          <ProfileGrunts user={user} onPosted={() => setUser((u) => u && (u.id === session.user?.id ? { ...u, gruntCount: u.gruntCount + 1 } : u))} />
        ) : (
          <People user={user} tab={tab} />
        )}
      </section>
    </div>
  );
}

function ProfileGrunts({ user, onPosted }: { user: User; onPosted: () => void }) {
  const session = useSession();
  const load = useCallback((before?: number) => api.userGrunts(user.handle, before), [user.handle]);
  const { grunts, more, error, loadMore, prepend } = usePagedGrunts(load);
  const mine = session.user?.id === user.id;

  return (
    <>
      {session.user && (
        <Composer
          key={user.id}
          prefill={mine ? "" : `@${user.handle} `}
          onPosted={(g) => {
            prepend(g);
            onPosted();
          }}
        />
      )}
      {error && <div className="error">{error}</div>}
      <GruntList grunts={grunts} more={more} onMore={loadMore} />
    </>
  );
}

function People({ user, tab }: { user: User; tab: "following" | "followers" }) {
  const [people, setPeople] = useState<User[] | null>(null);

  useEffect(() => {
    setPeople(null);
    (tab === "following" ? api.following(user.handle) : api.followers(user.handle)).then(setPeople, () => setPeople([]));
  }, [user.handle, user.followerCount, user.followingCount, tab]);

  if (!people) return null;
  return <UserCards users={people} onChange={(u) => setPeople((list) => list && replaceUser(list, u))} />;
}
