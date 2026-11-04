export type OrbState = "idle" | "available" | "taken";

const LABELS: Record<OrbState, string> = {
  idle: "",
  available: "That handle is available",
  taken: "That handle can't be used",
};

/**
 * A little glass ball with two fluids slowly flowing through each other: green and blue
 * when the handle is free, orange and red when it isn't. Always moving; the colours blend
 * across when the answer changes.
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
      <span className="orb-fluid orb-fluid-1" />
      <span className="orb-fluid orb-fluid-2" />
      <span className="orb-glass" />
    </span>
  );
}
