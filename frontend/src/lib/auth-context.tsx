"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import {
  login as loginCall,
  logout as logoutCall,
  refreshSession,
  register as registerCall,
  type AuthResponse,
  type LoginInput,
  type RegisterInput,
} from "@/lib/auth-api";
import { setAccessToken } from "@/lib/api-client";

type AuthState = {
  session: AuthResponse | null;
  initialized: boolean;
  login: (input: LoginInput) => Promise<void>;
  register: (input: RegisterInput) => Promise<void>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState<AuthResponse | null>(null);
  const [initialized, setInitialized] = useState(false);

  useEffect(() => {
    let cancelled = false;
    const stored =
      typeof window !== "undefined"
        ? window.localStorage.getItem("fixna.refreshToken")
        : null;
    if (!stored) {
      setInitialized(true);
      return;
    }
    refreshSession()
      .then((s) => {
        if (!cancelled) setSession(s);
      })
      .catch(() => {
        setAccessToken(null);
        if (typeof window !== "undefined") {
          window.localStorage.removeItem("fixna.refreshToken");
        }
      })
      .finally(() => {
        if (!cancelled) setInitialized(true);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (input: LoginInput) => {
    setSession(await loginCall(input));
  }, []);

  const register = useCallback(async (input: RegisterInput) => {
    setSession(await registerCall(input));
  }, []);

  const logout = useCallback(async () => {
    await logoutCall();
    setSession(null);
  }, []);

  const value = useMemo(
    () => ({ session, initialized, login, register, logout }),
    [session, initialized, login, register, logout],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
