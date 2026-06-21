import { create } from "zustand";
import { jwtDecode } from "jwt-decode";
import type { AuthState, TokenPayload, UserRole } from "@/types";

interface AuthStore extends AuthState {
  setAuth: (token: string, mustChangePassword: boolean) => void;
  clearAuth: () => void;
}

function parseToken(token: string): { role: UserRole; userId: string } | null {
  try {
    const payload = jwtDecode<TokenPayload>(token);
    if (payload.exp * 1000 < Date.now()) return null;
    return { role: payload.role, userId: payload.sub };
  } catch {
    return null;
  }
}

const storedToken = sessionStorage.getItem("accessToken");
const storedMustChange = sessionStorage.getItem("mustChangePassword") === "true";
const initialParsed = storedToken ? parseToken(storedToken) : null;

/**
 * Global authentication state.
 * Token is persisted in sessionStorage so it survives page refreshes
 * but is cleared when the browser tab is closed.
 * mustChangePassword is also persisted so a refresh mid-flow keeps the redirect.
 */
export const useAuthStore = create<AuthStore>((set) => ({
  accessToken: initialParsed ? storedToken : null,
  role: initialParsed?.role ?? null,
  userId: initialParsed?.userId ?? null,
  mustChangePassword: initialParsed ? storedMustChange : false,

  setAuth: (token: string, mustChangePassword: boolean) => {
    const parsed = parseToken(token);
    if (!parsed) return;
    sessionStorage.setItem("accessToken", token);
    sessionStorage.setItem("mustChangePassword", String(mustChangePassword));
    set({
      accessToken: token,
      role: parsed.role,
      userId: parsed.userId,
      mustChangePassword,
    });
  },

  clearAuth: () => {
    sessionStorage.removeItem("accessToken");
    sessionStorage.removeItem("mustChangePassword");
    set({ accessToken: null, role: null, userId: null, mustChangePassword: false });
  },
}));
