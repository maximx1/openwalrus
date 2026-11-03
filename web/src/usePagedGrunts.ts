import { useCallback, useEffect, useState } from "react";
import type { Grunt } from "./api.ts";

const PAGE_SIZE = 50; // GruntService.PageSize

/** A list of grunts that loads more on request and takes new ones at the top. */
export function usePagedGrunts(load: ((before?: number) => Promise<Grunt[]>) | null) {
  const [grunts, setGrunts] = useState<Grunt[]>([]);
  const [more, setMore] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!load) return;
    let live = true;
    setGrunts([]);
    load()
      .then((page) => {
        if (!live) return;
        setGrunts(page);
        setMore(page.length === PAGE_SIZE);
      })
      .catch((e: unknown) => live && setError(e instanceof Error ? e.message : "Could not load grunts."));
    return () => {
      live = false;
    };
  }, [load]);

  const loadMore = useCallback(async () => {
    const last = grunts[grunts.length - 1];
    if (!load || !last) return;
    const page = await load(last.createdAt);
    setGrunts((g) => [...g, ...page]);
    setMore(page.length === PAGE_SIZE);
  }, [grunts, load]);

  const prepend = useCallback((g: Grunt) => setGrunts((list) => [g, ...list]), []);

  return { grunts, more, error, loadMore, prepend };
}
