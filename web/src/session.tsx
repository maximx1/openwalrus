import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from "react";
import { api, type Session, type User } from "./api.ts";

type SessionState = Session & {
  loaded: boolean;
  setUser: (user: User | null) => void;
  refresh: () => Promise<void>;
};

const SessionContext = createContext<SessionState | null>(null);

export function SessionProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session>({ user: null, allowRegistration: true });
  const [loaded, setLoaded] = useState(false);

  const refresh = useCallback(async () => {
    try {
      setSession(await api.session());
    } finally {
      setLoaded(true);
    }
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const setUser = useCallback((user: User | null) => setSession((s) => ({ ...s, user })), []);

  return (
    <SessionContext.Provider value={{ ...session, loaded, setUser, refresh }}>{children}</SessionContext.Provider>
  );
}

export function useSession(): SessionState {
  const value = useContext(SessionContext);
  if (!value) throw new Error("useSession outside SessionProvider");
  return value;
}
