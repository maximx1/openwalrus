import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router";
import { api } from "../api.ts";
import { useSession } from "../session.tsx";

export function SignInPage() {
  const { user, setUser, allowRegistration } = useSession();
  const navigate = useNavigate();
  const [handle, setHandle] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to="/" replace />;

  async function submit() {
    setBusy(true);
    setError(null);
    try {
      setUser(await api.signIn(handle, password));
      navigate("/");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not sign in.");
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
      <h3>Log in</h3>
      {error && <div className="error">{error}</div>}
      <label>
        Handle
        <input name="handle" autoComplete="username" required value={handle} onChange={(e) => setHandle(e.target.value)} />
      </label>
      <label>
        Password
        <input
          name="password"
          type="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
      </label>
      <button type="submit" disabled={busy}>
        Log in
      </button>
      {allowRegistration && (
        <p className="hint">
          New here? <Link to="/signup">Sign up</Link>
        </p>
      )}
    </form>
  );
}
