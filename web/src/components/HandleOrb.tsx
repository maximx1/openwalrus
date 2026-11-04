export type OrbState = "idle" | "available" | "taken";

const LABELS: Record<OrbState, string> = {
  idle: "",
  available: "That handle is available",
  taken: "That handle can't be used",
};

/**
 * A little lava lamp: two fluids slowly mixing, green and blue when the handle is free,
 * orange and red when it isn't. Always moving; the colours blend across when it changes.
 */
export function HandleOrb({ state }: { state: OrbState }) {
  return (
    <span
      className={`orb orb-${state}`}
      role={state === "idle" ? undefined : "img"}
      aria-hidden={state === "idle"}
      aria-label={LABELS[state] || undefined}
      title={LABELS[state] || undefined}
    >
      <span className="orb-swirl" />
      <span className="orb-blob orb-blob-1" />
      <span className="orb-blob orb-blob-2" />
      <span className="orb-blob orb-blob-3" />
      <span className="orb-glass" />
    </span>
  );
}
