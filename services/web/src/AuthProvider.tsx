import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { api, setUnauthorizedHandler } from "./api";
import { clearSession, rememberSession, type AuthMe } from "./auth";

type AuthApi = {
  user: AuthMe | null;
  status: "loading" | "ready";
  login: (email: string, password: string) => Promise<AuthMe>;
  signup: (email: string, password: string, displayName: string) => Promise<AuthMe>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthApi | null>(null);

export function useAuth(): AuthApi {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth needs AuthProvider");
  }
  return ctx;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthMe | null>(null);
  const [status, setStatus] = useState<AuthApi["status"]>("loading");

  const apply = useCallback((me: AuthMe | null) => {
    setUser(me);
    if (me) {
      rememberSession(me);
    } else {
      clearSession();
    }
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(() => apply(null));
    api
      .get<AuthMe>("/api/auth/me", { redirectOn401: false })
      .then((me) => apply(me))
      .catch(() => apply(null))
      .finally(() => setStatus("ready"));
    return () => setUnauthorizedHandler(null);
  }, [apply]);

  const login = useCallback(
    async (email: string, password: string) => {
      const me = await api.post<AuthMe>(
        "/api/auth/login",
        { email, password },
        { auth: false, redirectOn401: false },
      );
      apply(me);
      return me;
    },
    [apply],
  );

  const signup = useCallback(
    async (email: string, password: string, displayName: string) => {
      const me = await api.post<AuthMe>(
        "/api/auth/signup",
        { email, password, displayName },
        { auth: false, redirectOn401: false },
      );
      apply(me);
      return me;
    },
    [apply],
  );

  const logout = useCallback(async () => {
    try {
      await api.post("/api/auth/logout", undefined, { redirectOn401: false });
    } finally {
      apply(null);
    }
  }, [apply]);

  const value = useMemo(
    () => ({ user, status, login, signup, logout }),
    [user, status, login, signup, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
