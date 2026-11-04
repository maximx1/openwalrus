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
  /** How many people other than the author have seen it, signed in or not. */
  views: number;
  /** Set on a reply: the grunt it answers. */
  replyTo?: string;
  replyCount: number;
  regruntCount: number;
  regruntedByMe: boolean;
  /** In a feed: who regrunted it into the feed. */
  regruntedBy?: Author;
  /** When it arrived in the feed: posted, or regrunted. */
  feedAt: number;
};

/** A picture or video in a grunt, and how big a file it is. */
export type MediaStats = { kind: "image" | "video"; url: string; bytes: number; contentType: string };

/** The numbers behind a grunt, for its Stats dialog. */
export type GruntStats = {
  views: number;
  signedInViews: number;
  anonymousViews: number;
  /** Of the anonymous views, those through an embed on another site. */
  embedViews: number;
  /** Where it's embedded, most views first; site is "" when the browser didn't say. */
  embeds: { site: string; views: number }[];
  replies: number;
  regrunts: number;
  characters: number;
  media: MediaStats[];
};

/** One page of a feed; `next` fetches the page after, if there is one. */
export type FeedPage = { grunts: Grunt[]; next?: number };

export type Session = { user: User | null; allowRegistration: boolean };

/** A browser you're signed in on. */
export type SignedInSession = {
  id: string;
  /** The one making this request. */
  current: boolean;
  createdAt: number;
  lastSeenAt: number;
  userAgent: string;
  address: string;
};

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
  if (!res.ok) throw new ApiError(errorMessage(data, res.status), res.status);
  return data as T;
}

function errorMessage(data: unknown, status: number): string {
  const error = data && typeof data === "object" && "error" in data ? (data as { error: unknown }).error : null;
  if (typeof error === "string") return error;
  if (status === 413) return "That file is too big.";
  return `Something went wrong (${status}).`;
}

/** Uploads a file as the form field `field`, calling onProgress with 0 to 1 as it goes (fetch can't report that). */
function upload(path: string, field: string, file: File, onProgress?: (fraction: number) => void): Promise<{ id: string }> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open("POST", `/api${path}`);
    xhr.setRequestHeader("X-Requested-With", "openwalrus");
    xhr.setRequestHeader("Accept", "application/json");
    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable) onProgress?.(e.loaded / e.total);
    };
    xhr.onload = () => {
      let data: unknown = null;
      try {
        data = JSON.parse(xhr.responseText);
      } catch {
        // not JSON; errorMessage falls back to the status
      }
      if (xhr.status >= 200 && xhr.status < 300) resolve(data as { id: string });
      else reject(new ApiError(errorMessage(data, xhr.status), xhr.status));
    };
    xhr.onerror = () => reject(new ApiError("The upload didn't get through.", 0));
    const form = new FormData();
    form.append(field, file);
    xhr.send(form);
  });
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
    request<FeedPage>("GET", `/users/${enc(handle)}/grunts${before ? `?before=${before}` : ""}`),
  followers: (handle: string) => request<User[]>("GET", `/users/${enc(handle)}/followers`),
  following: (handle: string) => request<User[]>("GET", `/users/${enc(handle)}/following`),
  follow: (handle: string) => request<User>("PUT", `/users/${enc(handle)}/follow`),
  unfollow: (handle: string) => request<User>("DELETE", `/users/${enc(handle)}/follow`),
  setProfileImage: (imageId: string) => request<void>("PUT", "/me/profile-image", { imageId }),
  setBannerImage: (imageId: string) => request<void>("PUT", "/me/banner-image", { imageId }),
  changePassword: (current: string, next: string) => request<void>("PUT", "/me/password", { current, next }),
  sessions: () => request<SignedInSession[]>("GET", "/me/sessions"),
  endSession: (id: string) => request<void>("DELETE", `/me/sessions/${enc(id)}`),
  endOtherSessions: () => request<void>("DELETE", "/me/sessions"),

  feed: (before?: number) => request<FeedPage>("GET", `/feed${before ? `?before=${before}` : ""}`),
  grunt: (id: string) => request<Grunt>("GET", `/grunts/${enc(id)}`),
  gruntStats: (id: string) => request<GruntStats>("GET", `/grunts/${enc(id)}/stats`),
  replies: (id: string) => request<Grunt[]>("GET", `/grunts/${enc(id)}/replies`),
  reply: (id: string, message: string) => request<Grunt>("POST", `/grunts/${enc(id)}/replies`, { message }),
  regrunt: (id: string) => request<Grunt>("PUT", `/grunts/${enc(id)}/regrunt`),
  unregrunt: (id: string) => request<Grunt>("DELETE", `/grunts/${enc(id)}/regrunt`),
  setFriendsOnly: (id: string, friendsOnly: boolean) => request<Grunt>("PATCH", `/grunts/${enc(id)}`, { friendsOnly }),
  hot: () => request<Grunt[]>("GET", "/hot"),
  postGrunt: (message: string, friendsOnly: boolean) => request<Grunt>("POST", "/grunts", { message, friendsOnly }),
  /** keepalive, so the last batch still goes out as the page closes. */
  recordViews: (seen: { ids: string[]; visitor: string; embed?: boolean; site?: string }) =>
    request<void>("POST", "/views", seen, true),
  deleteGrunt: (id: string) => request<void>("DELETE", `/grunts/${enc(id)}`),

  uploadImage: (file: File, onProgress?: (fraction: number) => void) => upload("/images", "picture", file, onProgress),
  uploadVideo: (file: File, onProgress?: (fraction: number) => void) => upload("/videos", "video", file, onProgress),
};

export const videoUrl = (id: string) => `/api/videos/${id}`;

/** The page other sites put in an iframe to show a grunt. */
export const embedPath = (id: string) => `/embed/${id}`;

/** A grunt's own page. (A reply's sends you on to the grunt it answers.) */
export const gruntPath = (g: Grunt) => `/${g.author.handle}/grunts/${g.id}`;

export const imageUrl = (id: string | undefined, thumb = false) =>
  id ? `/api/images/${id}${thumb ? "/thumb" : ""}` : "/noimage.svg";
