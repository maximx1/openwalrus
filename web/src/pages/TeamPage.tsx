import { useEffect, useState } from "react";
import { api, type User } from "../api.ts";
import { replaceUser, UserCards } from "../components/UserCards.tsx";
import { useSession } from "../session.tsx";

export function TeamPage() {
  const session = useSession();
  const [users, setUsers] = useState<User[] | null>(null);

  useEffect(() => {
    api.users().then(setUsers, () => setUsers([]));
  }, [session.user?.id]);

  return (
    <>
      <h2 className="page-title">{session.user ? "Welcome to the winning team" : "Come join the winning team"}</h2>
      {users && <UserCards users={users} onChange={(u) => setUsers((list) => list && replaceUser(list, u))} />}
    </>
  );
}
