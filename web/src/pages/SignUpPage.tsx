import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router";
import { api } from "../api.ts";
import { useSession } from "../session.tsx";

export function SignUpPage() {
  const { user, setUser, allowRegistration } = useSession();
  const navigate = useNavigate();
  const [fullName, setFullName] = useState("");
  const [handle, setHandle] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

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
      navigate(`/${created.handle}`);
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
        <input
          name="handle"
          autoComplete="username"
          required
          maxLength={30}
          pattern="@?[A-Za-z0-9_]+"
          title="Letters, numbers and underscores"
          value={handle}
          onChange={(e) => setHandle(e.target.value)}
        />
      </label>
      <label>
        Password
        <input
          name="password"
          type="password"
          autoComplete="new-password"
          required
          minLength={8}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
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
