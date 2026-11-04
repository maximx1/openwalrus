import { useCallback, useEffect, useState } from "react";
import type { FeedPage, Grunt } from "./api.ts";

/** A feed that loads more on request, takes new grunts at the top, and updates grunts in place. */
export function usePagedGrunts(load: ((before?: number) => Promise<FeedPage>) | null) {
  const [grunts, setGrunts] = useState<Grunt[]>([]);
  const [next, setNext] = useState<number | undefined>();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!load) return;
    let live = true;
    setGrunts([]);
    setNext(undefined);
    load()
      .then((page) => {
        if (!live) return;
        setGrunts(page.grunts);
        setNext(page.next);
      })
      .catch((e: unknown) => live && setError(e instanceof Error ? e.message : "Could not load grunts."));
    return () => {
      live = false;
    };
  }, [load]);

  const loadMore = useCallback(async () => {
    if (!load || next === undefined) return;
    const page = await load(next);
    // A grunt regrunted again later can turn up on two pages; show it once.
    setGrunts((list) => [...list, ...page.grunts.filter((g) => !list.some((x) => x.id === g.id))]);
    setNext(page.next);
  }, [load, next]);

  const prepend = useCallback((g: Grunt) => setGrunts((list) => [g, ...list]), []);
  const remove = useCallback((id: string) => setGrunts((list) => list.filter((g) => g.id !== id)), []);
  /** A grunt changed (regrunted, replied to, made friends-only): keep how it came into this feed. */
  const replace = useCallback(
    (g: Grunt) =>
      setGrunts((list) => list.map((x) => (x.id === g.id ? { ...g, regruntedBy: x.regruntedBy, feedAt: x.feedAt } : x))),
    [],
  );

  return { grunts, more: next !== undefined, error, loadMore, prepend, remove, replace };
}
