import { useEffect, useState } from "react";
import { api, type Grunt } from "../api.ts";
import { GruntList } from "../components/Grunts.tsx";
import { useSession } from "../session.tsx";

/** The grunts getting the most looks this week, newer ones counting for more (GruntService.hot). */
export function HotPage() {
  const session = useSession();
  const [grunts, setGrunts] = useState<Grunt[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.hot().then(setGrunts, (e: unknown) => setError(e instanceof Error ? e.message : "Could not load grunts."));
  }, [session.user?.id]);

  return (
    <div className="narrow">
      <h2 className="page-title">What's hot</h2>
      <p className="page-subtitle">The grunts people are looking at most this week. Newer ones need fewer views to climb.</p>
      {error && <div className="error">{error}</div>}
      {grunts &&
        (grunts.length === 0 ? (
          <p className="empty">Nothing has caught anyone's eye this week yet.</p>
        ) : (
          <GruntList
            grunts={grunts}
            more={false}
            onMore={() => {}}
            onDeleted={(g) => setGrunts((list) => list && list.filter((x) => x.id !== g.id))}
          />
        ))}
    </div>
  );
}
