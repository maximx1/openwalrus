import { Link, NavLink, Outlet, useNavigate } from "react-router";
import { api } from "../api.ts";
import { useSession } from "../session.tsx";

export function Layout() {
  const { user, loaded, allowRegistration, setUser } = useSession();
  const navigate = useNavigate();

  async function signOut() {
    await api.signOut();
    setUser(null);
    navigate("/");
  }

  return (
    <>
      <nav className="navbar">
        <div className="nav-side">
          <NavLink to="/team">Team</NavLink>
        </div>
        <Link to="/" className="brand" aria-label="OpenWalrus home">
          <span className="brand-dot" />
          <span className="brand-name">OpenWalrus</span>
        </Link>
        <div className="nav-side nav-right">
          {!loaded ? null : user ? (
            <>
              <NavLink to={`/${user.handle}`}>@{user.handle}</NavLink>
              <button type="button" className="link-button" onClick={signOut}>
                Log out
              </button>
            </>
          ) : (
            <>
              {allowRegistration && <NavLink to="/signup">Sign up</NavLink>}
              <NavLink to="/login">Log in</NavLink>
            </>
          )}
        </div>
      </nav>
      <main>{loaded ? <Outlet /> : null}</main>
    </>
  );
}
