import { useCallback, useEffect, useState } from "react";
import { Navigate } from "react-router";
import { api, type SignedInSession } from "../api.ts";
import { useSession } from "../session.tsx";

/** Your password, and the browsers you're signed in on. */
export function SettingsPage() {
  const { user } = useSession();
  if (!user) return <Navigate to="/login" replace />;
  return (
    <div className="narrow settings">
      <h2 className="page-title">Settings</h2>
      <PasswordForm />
      <Sessions />
    </div>
  );
}

function PasswordForm() {
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [again, setAgain] = useState("");
  const [message, setMessage] = useState<{ ok: boolean; text: string } | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit() {
    if (next !== again) {
      setMessage({ ok: false, text: "The new passwords don't match." });
      return;
    }
    setBusy(true);
    setMessage(null);
    try {
      await api.changePassword(current, next);
      setCurrent("");
      setNext("");
      setAgain("");
      setMessage({ ok: true, text: "Password changed. You've been signed out everywhere else." });
    } catch (e) {
      setMessage({ ok: false, text: e instanceof Error ? e.message : "Could not change it." });
    } finally {
      setBusy(false);
    }
  }

  return (
    <form
      className="auth-form card settings-section"
      onSubmit={(e) => {
        e.preventDefault();
        void submit();
      }}
    >
      <h3>Change your password</h3>
      {message && <div className={message.ok ? "notice" : "error"}>{message.text}</div>}
      <label>
        Current password
        <input type="password" autoComplete="current-password" required value={current} onChange={(e) => setCurrent(e.target.value)} />
      </label>
      <label>
        New password
        <input
          type="password"
          autoComplete="new-password"
          required
          minLength={10}
          maxLength={200}
          value={next}
          onChange={(e) => setNext(e.target.value)}
        />
        <span className="field-hint">At least 10 characters. A few words strung together works well.</span>
      </label>
      <label>
        New password again
        <input type="password" autoComplete="new-password" required value={again} onChange={(e) => setAgain(e.target.value)} />
      </label>
      <button type="submit" disabled={busy}>
        Change password
      </button>
    </form>
  );
}

function Sessions() {
  const { setUser } = useSession();
  const [list, setList] = useState<SignedInSession[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    api.sessions().then(setList, (e: unknown) => setError(e instanceof Error ? e.message : "Could not load them."));
  }, []);
  useEffect(load, [load]);

  async function end(s: SignedInSession) {
    try {
      await api.endSession(s.id);
      if (s.current) setUser(null);
      else load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not sign that out.");
      load();
    }
  }

  async function endOthers() {
    if (!window.confirm("Sign out everywhere except here?")) return;
    await api.endOtherSessions();
    load();
  }

  const others = list?.filter((s) => !s.current).length ?? 0;

  return (
    <section className="card settings-section sessions">
      <h3>Where you're signed in</h3>
      <p className="hint">
        Each browser stays signed in until it's left unused for 30 days, or for 90 days at most. Don't recognise one? Sign it out
        and change your password.
      </p>
      {error && <div className="error">{error}</div>}
      <ul>
        {list?.map((s) => (
          <li key={s.id}>
            <div>
              <strong>{describeDevice(s.userAgent)}</strong>
              {s.current && <span className="this-device">This browser</span>}
              <div className="hint">
                {s.address && <>{s.address} · </>}
                {s.current ? "active now" : `last active ${since(s.lastSeenAt)}`} · signed in {since(s.createdAt)}
              </div>
            </div>
            <button type="button" className="secondary" onClick={() => void end(s)}>
              Sign out
            </button>
          </li>
        ))}
      </ul>
      {others > 0 && (
        <button type="button" className="secondary" onClick={() => void endOthers()}>
          Sign out everywhere else
        </button>
      )}
    </section>
  );
}

/** "Firefox on Linux", near enough, from a user agent string. */
function describeDevice(ua: string): string {
  const browser = /Edg\//.test(ua)
    ? "Edge"
    : /OPR\//.test(ua)
      ? "Opera"
      : /Firefox\//.test(ua)
        ? "Firefox"
        : /Chrome\//.test(ua)
          ? "Chrome"
          : /Safari\//.test(ua)
            ? "Safari"
            : null;
  const os = /iPhone|iPad/.test(ua)
    ? "iOS"
    : /Android/.test(ua)
      ? "Android"
      : /Windows/.test(ua)
        ? "Windows"
        : /Mac OS X/.test(ua)
          ? "macOS"
          : /CrOS/.test(ua)
            ? "ChromeOS"
            : /Linux/.test(ua)
              ? "Linux"
              : null;
  if (browser && os) return `${browser} on ${os}`;
  return browser ?? os ?? (ua ? ua.slice(0, 60) : "Unknown browser");
}

function since(ms: number): string {
  const minutes = Math.round((Date.now() - ms) / 60000);
  if (minutes < 2) return "just now";
  if (minutes < 60) return `${minutes} minutes ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} ${hours === 1 ? "hour" : "hours"} ago`;
  return new Date(ms).toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
}
