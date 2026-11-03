import type { User } from "../api.ts";
import { ProfileCard } from "./ProfileCard.tsx";

export function UserCards({ users, onChange }: { users: User[]; onChange: (user: User) => void }) {
  if (users.length === 0) return <p className="empty">Nobody here yet.</p>;
  return (
    <div className="card-grid">
      {users.map((u) => (
        <ProfileCard key={u.id} user={u} onChange={onChange} />
      ))}
    </div>
  );
}

/** Replaces one user in a list, for after a follow. */
export const replaceUser = (list: User[], next: User) => list.map((u) => (u.id === next.id ? next : u));
