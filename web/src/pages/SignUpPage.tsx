import { useEffect, useState } from "react";
import { Link, Navigate, useNavigate } from "react-router";
import { api } from "../api.ts";
import { HandleOrb, type OrbState } from "../components/HandleOrb.tsx";
import { useSession } from "../session.tsx";

export function SignUpPage() {
  const { user, setUser, allowRegistration } = useSession();
  const navigate = useNavigate();
  const [fullName, setFullName] = useState("");
  const [handle, setHandle] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const availability = useHandleAvailability(handle);

  if (user) return <Navigate to="/" replace />;
  if (!allowRegistration)
    return (
      <div className="auth-form card">
        <h3>Sign-ups are closed</h3>
        <p>Ask whoever runs this OpenWalrus to let you in.</p>
        <Link to="/login">Log in</Link>
      </div>
    );

  async function submit() {
    setBusy(true);
    setError(null);
    try {
      const created = await api.signUp(fullName, handle, password);
      setUser(created);
      navigate("/");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not sign up.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <form
      className="auth-form card"
      onSubmit={(e) => {
        e.preventDefault();
        void submit();
      }}
    >
      <h3>Join OpenWalrus today.</h3>
      {error && <div className="error">{error}</div>}
      <label>
        Full name
        <input name="fullName" autoComplete="name" required maxLength={60} value={fullName} onChange={(e) => setFullName(e.target.value)} />
      </label>
      <label>
        Handle
        <span className="input-with-orb">
          <input
            name="handle"
            autoComplete="username"
            required
            maxLength={31}
            aria-describedby="handle-status"
            aria-invalid={availability.state === "taken"}
            value={handle}
            onChange={(e) => setHandle(e.target.value)}
          />
          <HandleOrb state={availability.state} />
        </span>
        <span id="handle-status" className="field-note" aria-live="polite">
          {availability.state === "taken" ? availability.reason : ""}
        </span>
      </label>
      <label>
        Password
        <input
          name="password"
          type="password"
          autoComplete="new-password"
          required
          minLength={10}
          maxLength={200}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <span className="field-hint">At least 10 characters. A few words strung together works well.</span>
      </label>
      <button type="submit" disabled={busy}>
        Sign me up
      </button>
      <p className="hint">
        Have an account? <Link to="/login">Log in</Link>
      </p>
    </form>
  );
}

/** Asks the server whether a handle is free, a moment after typing stops. */
function useHandleAvailability(raw: string): { state: OrbState; reason: string } {
  const handle = raw.trim().replace(/^@/, "");
  const [result, setResult] = useState<{ handle: string; state: OrbState; reason: string }>({
    handle: "",
    state: "idle",
    reason: "",
  });

  useEffect(() => {
    if (!handle) return;
    let live = true;
    const timer = setTimeout(() => {
      api.handleAvailability(handle).then(
        (r) => live && setResult({ handle, state: r.available ? "available" : "taken", reason: r.reason ?? "" }),
        () => {}, // no answer: leave the orb as it was
      );
    }, 300);
    return () => {
      live = false;
      clearTimeout(timer);
    };
  }, [handle]);

  // While a new answer is on its way, keep showing the last one so the orb doesn't flicker.
  return handle ? result : { state: "idle", reason: "" };
}
