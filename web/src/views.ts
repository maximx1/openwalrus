import { api } from "./api.ts";

// A grunt counts as seen once at least half of it (or, for a tall one, half the screen's
// worth of it) has been on screen for a second. Seen ids
// are sent in batches; the server counts each person once, so resending is harmless, but
// each id goes only once per page load anyway.

const DWELL_MS = 1000;
const FLUSH_MS = 3000;

const sent = new Set<string>();
const queued = new Set<string>();
const timers = new Map<Element, number>();
const ids = new WeakMap<Element, string>();
let flushTimer: number | undefined;

function flush() {
  flushTimer = undefined;
  if (queued.size === 0) return;
  const batch = [...queued];
  queued.clear();
  batch.forEach((id) => sent.add(id));
  api.recordViews(batch).catch(() => batch.forEach((id) => sent.delete(id)));
}

function seen(id: string) {
  if (sent.has(id) || queued.has(id)) return;
  queued.add(id);
  flushTimer ??= window.setTimeout(flush, FLUSH_MS);
}

const observer =
  typeof IntersectionObserver === "undefined"
    ? null
    : new IntersectionObserver(
        (entries) => {
          for (const e of entries) {
            const id = ids.get(e.target);
            if (!id) continue;
            const enough =
              e.intersectionRatio >= 0.5 ||
              (e.rootBounds !== null && e.intersectionRect.height >= e.rootBounds.height / 2);
            if (enough && !timers.has(e.target)) {
              timers.set(e.target, window.setTimeout(() => seen(id), DWELL_MS));
            } else if (!enough) {
              window.clearTimeout(timers.get(e.target));
              timers.delete(e.target);
            }
          }
        },
        { threshold: [0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1] },
      );

// Send what's waiting when the page is hidden or closed, rather than lose it.
document.addEventListener("visibilitychange", () => {
  if (document.visibilityState === "hidden") flush();
});
window.addEventListener("pagehide", flush);

/** Watches a grunt's element; returns the function that stops watching it. */
export function watchForView(el: Element, gruntId: string): () => void {
  if (!observer || sent.has(gruntId)) return () => {};
  ids.set(el, gruntId);
  observer.observe(el);
  return () => {
    observer.unobserve(el);
    window.clearTimeout(timers.get(el));
    timers.delete(el);
  };
}
