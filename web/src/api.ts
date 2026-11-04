// Every call the web client makes to the Play app. The shapes mirror models/Views.scala.

export type User = {
  id: string;
  handle: string;
  fullName: string;
  // Left out of the JSON when there is none.
  profileImage?: string;
  bannerImage?: string;
  gruntCount: number;
  followingCount: number;
  followerCount: number;
  followedByMe: boolean;
};

export type Author = Pick<User, "id" | "handle" | "fullName" | "profileImage">;

export type Grunt = {
  id: string;
  author: Author;
  /** Already rendered and cleaned by the server. */
  html: string;
  createdAt: number;
  /** Seen only by the author and the people they follow. */
  friendsOnly: boolean;
  /** How many people other than the author have seen it. */
  views: number;
};

export type Session = { user: User | null; allowRegistration: boolean };

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
  }
}

async function request<T>(method: string, path: string, body?: unknown, keepalive = false): Promise<T> {
  const headers: Record<string, string> = {
    // Lets the request past Play's CSRF filter; a page on another site cannot add it.
    "X-Requested-With": "openwalrus",
    Accept: "application/json",
  };
  let payload: BodyInit | undefined;
  if (body instanceof FormData) {
    payload = body;
  } else if (body !== undefined) {
    headers["Content-Type"] = "application/json";
    payload = JSON.stringify(body);
  }
  const res = await fetch(`/api${path}`, { method, headers, body: payload, credentials: "same-origin", keepalive });
  if (res.status === 204) return undefined as T;
  const data: unknown = await res.json().catch(() => null);
  if (!res.ok) {
    const message =
      data && typeof data === "object" && "error" in data
        ? String((data as { error: unknown }).error)
        : `Something went wrong (${res.status}).`;
    throw new ApiError(message, res.status);
  }
  return data as T;
}

const enc = encodeURIComponent;

export const api = {
  session: () => request<Session>("GET", "/session"),
  signIn: (handle: string, password: string) => request<User>("POST", "/session", { handle, password }),
  signOut: () => request<void>("DELETE", "/session"),
  signUp: (fullName: string, handle: string, password: string) =>
    request<User>("POST", "/users", { fullName, handle, password }),

  handleAvailability: (handle: string) =>
    request<{ available: boolean; reason?: string }>("GET", `/handles/${enc(handle)}`),
  users: () => request<User[]>("GET", "/users"),
  user: (handle: string) => request<User>("GET", `/users/${enc(handle)}`),
  userGrunts: (handle: string, before?: number) =>
    request<Grunt[]>("GET", `/users/${enc(handle)}/grunts${before ? `?before=${before}` : ""}`),
  followers: (handle: string) => request<User[]>("GET", `/users/${enc(handle)}/followers`),
  following: (handle: string) => request<User[]>("GET", `/users/${enc(handle)}/following`),
  follow: (handle: string) => request<User>("PUT", `/users/${enc(handle)}/follow`),
  unfollow: (handle: string) => request<User>("DELETE", `/users/${enc(handle)}/follow`),
  setProfileImage: (imageId: string) => request<void>("PUT", "/me/profile-image", { imageId }),
  setBannerImage: (imageId: string) => request<void>("PUT", "/me/banner-image", { imageId }),

  feed: (before?: number) => request<Grunt[]>("GET", `/feed${before ? `?before=${before}` : ""}`),
  hot: () => request<Grunt[]>("GET", "/hot"),
  postGrunt: (message: string, friendsOnly: boolean) => request<Grunt>("POST", "/grunts", { message, friendsOnly }),
  /** keepalive, so the last batch still goes out as the page closes. */
  recordViews: (ids: string[]) => request<void>("POST", "/views", { ids }, true),
  deleteGrunt: (id: string) => request<void>("DELETE", `/grunts/${enc(id)}`),

  uploadImage: (file: File) => {
    const form = new FormData();
    form.append("picture", file);
    return request<{ id: string }>("POST", "/images", form);
  },
};

export const imageUrl = (id: string | undefined, thumb = false) =>
  id ? `/api/images/${id}${thumb ? "/thumb" : ""}` : "/noimage.svg";
