import { useRef, useState, type CSSProperties } from "react";
import { Link } from "react-router";
import { api, imageUrl, type User } from "../api.ts";
import { useSession } from "../session.tsx";

type Props = {
  user: User;
  /** Called with the user as they now stand after a follow or a new picture. */
  onChange?: (user: User) => void;
};

/** Banner, picture, name and counts. On your own card, the banner and picture can be changed. */
export function ProfileCard({ user, onChange }: Props) {
  const session = useSession();
  const mine = session.user?.id === user.id;

  return (
    <div className="card">
      <div className="profile-card">
        <ImageTarget
          enabled={mine}
          label="Change banner"
          className="banner"
          style={user.bannerImage ? { backgroundImage: `url(${imageUrl(user.bannerImage)})` } : undefined}
          onUploaded={async (id) => {
            await api.setBannerImage(id);
            const next = { ...user, bannerImage: id };
            onChange?.(next);
            if (mine) session.setUser({ ...session.user!, bannerImage: id });
          }}
        />
        <div className="profile-head">
          <ImageTarget
            enabled={mine}
            label="Change picture"
            className="avatar-large"
            image={imageUrl(user.profileImage, true)}
            onUploaded={async (id) => {
              await api.setProfileImage(id);
              onChange?.({ ...user, profileImage: id });
              if (mine) session.setUser({ ...session.user!, profileImage: id });
            }}
          />
          <div className="profile-names">
            <Link to={`/${user.handle}`} className="name">
              {user.fullName}
            </Link>
            <div className="handle">@{user.handle}</div>
          </div>
        </div>
        <div className="counts">
          <Link to={`/${user.handle}`}>
            Grunts<strong>{user.gruntCount}</strong>
          </Link>
          <Link to={`/${user.handle}/following`}>
            Following<strong>{user.followingCount}</strong>
          </Link>
          <Link to={`/${user.handle}/followers`}>
            Followers<strong>{user.followerCount}</strong>
          </Link>
        </div>
      </div>
      <FollowButton user={user} onChange={onChange} />
    </div>
  );
}

export function FollowButton({ user, onChange }: Props) {
  const session = useSession();
  const me = session.user;
  const [busy, setBusy] = useState(false);
  if (!me || me.id === user.id) return null;

  async function toggle() {
    setBusy(true);
    try {
      onChange?.(user.followedByMe ? await api.unfollow(user.handle) : await api.follow(user.handle));
      await session.refresh(); // your own Following count changed too
    } finally {
      setBusy(false);
    }
  }

  return (
    <button type="button" className={`follow-button${user.followedByMe ? " following" : ""}`} disabled={busy} onClick={toggle}>
      {user.followedByMe ? "Unfollow" : "Follow"}
    </button>
  );
}

type ImageTargetProps = {
  enabled: boolean;
  label: string;
  className: string;
  image?: string;
  style?: CSSProperties;
  onUploaded: (imageId: string) => Promise<void>;
};

/** A picture that, when it's yours, opens a file chooser and uploads what you pick. */
function ImageTarget({ enabled, label, className, image, style, onUploaded }: ImageTargetProps) {
  const input = useRef<HTMLInputElement>(null);
  const [state, setState] = useState<"idle" | "uploading" | string>("idle");

  const content = image ? <img src={image} alt="" /> : null;
  if (!enabled) return <div className={className} style={style}>{content}</div>;

  async function upload(file: File | undefined) {
    if (!file) return;
    setState("uploading");
    try {
      const { id } = await api.uploadImage(file);
      await onUploaded(id);
      setState("idle");
    } catch (e) {
      setState(e instanceof Error ? e.message : "Upload failed.");
    }
  }

  return (
    <button
      type="button"
      className={`${className} editable`}
      style={style}
      title={typeof state === "string" && state !== "idle" && state !== "uploading" ? state : label}
      aria-label={label}
      onClick={() => input.current?.click()}
    >
      {content}
      {state === "uploading" && <span className="image-status">Uploading…</span>}
      <input
        ref={input}
        type="file"
        accept="image/png,image/jpeg,image/gif"
        hidden
        onChange={(e) => {
          void upload(e.target.files?.[0]);
          e.target.value = "";
        }}
      />
    </button>
  );
}
