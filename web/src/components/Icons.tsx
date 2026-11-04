// Small line icons for the grunt actions, drawn in the text colour.

const props = {
  width: 16,
  height: 16,
  viewBox: "0 0 16 16",
  fill: "none",
  stroke: "currentColor",
  strokeWidth: 1.5,
  strokeLinecap: "round" as const,
  strokeLinejoin: "round" as const,
  "aria-hidden": true,
};

export const ReplyIcon = () => (
  <svg {...props}>
    <path d="M2.5 3.5h11v7h-6l-3 2.5v-2.5h-2z" />
  </svg>
);

export const RegruntIcon = () => (
  <svg {...props}>
    <path d="M3 7V5.5A1.5 1.5 0 0 1 4.5 4H12M10 2l2 2-2 2M13 9v1.5a1.5 1.5 0 0 1-1.5 1.5H4M6 14l-2-2 2-2" />
  </svg>
);

export const ShareIcon = () => (
  <svg {...props}>
    <path d="M8 2v8M5 5l3-3 3 3M3.5 8.5v4.5h9V8.5" />
  </svg>
);

export const DotsIcon = () => (
  <svg {...props} fill="currentColor" stroke="none">
    <circle cx="3" cy="8" r="1.4" />
    <circle cx="8" cy="8" r="1.4" />
    <circle cx="13" cy="8" r="1.4" />
  </svg>
);

/** An on/off switch drawing, for inside a button that carries the state (role="switch"). */
export const Switch = ({ on }: { on: boolean }) => <span className={`switch${on ? " on" : ""}`} aria-hidden="true" />;

export const CloseIcon = () => (
  <svg {...props}>
    <path d="M4 4l8 8M12 4l-8 8" />
  </svg>
);
