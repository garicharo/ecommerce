const AUTHED_KEY = "authed";
const ROLE_KEY = "role";
const NAME_KEY = "displayName";

export type Role = "ADMIN" | "SHOPPER";

export type AuthMe = {
  email: string;
  role: Role;
  displayName: string;
};

export function getRole(): Role | null {
  const value = sessionStorage.getItem(ROLE_KEY);
  return value === "ADMIN" || value === "SHOPPER" ? value : null;
}

export function setRole(role: Role | null): void {
  if (role) {
    sessionStorage.setItem(ROLE_KEY, role);
  } else {
    sessionStorage.removeItem(ROLE_KEY);
  }
}

export function getDisplayName(): string | null {
  return sessionStorage.getItem(NAME_KEY);
}

export function setDisplayName(name: string | null): void {
  if (name) {
    sessionStorage.setItem(NAME_KEY, name);
  } else {
    sessionStorage.removeItem(NAME_KEY);
  }
}

export function shortName(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  const first = parts[0] ?? "";
  if (parts.length === 1) {
    return first.length > 14 ? `${first.slice(0, 13)}…` : first;
  }
  const last = parts[1] ?? "";
  const initial = last.slice(0, 1).toUpperCase();
  return initial ? `${first} ${initial}.` : first;
}

export function rememberSession(me: AuthMe): void {
  sessionStorage.setItem(AUTHED_KEY, "1");
  setRole(me.role === "ADMIN" ? "ADMIN" : "SHOPPER");
  setDisplayName(me.displayName);
}

export function clearSession(): void {
  sessionStorage.removeItem(AUTHED_KEY);
  sessionStorage.removeItem("basic");
  sessionStorage.removeItem(ROLE_KEY);
  sessionStorage.removeItem(NAME_KEY);
}

export function isLoggedIn(): boolean {
  return sessionStorage.getItem(AUTHED_KEY) === "1";
}

export function loginPath(next?: string): string {
  const dest = next ?? `${window.location.pathname}${window.location.search}`;
  return `/login?next=${encodeURIComponent(dest)}`;
}
