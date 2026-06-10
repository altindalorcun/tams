import { create } from "zustand";
import { jwtDecode } from "jwt-decode";
import type { AuthState, TokenPayload, UserRole } from "@/types";

interface AuthStore extends AuthState {
  setToken: (token: string) => void;
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
const initialParsed = storedToken ? parseToken(storedToken) : null;

/**
 * Global authentication state.
 * Token is persisted in sessionStorage so it survives page refreshes
 * but is cleared when the browser tab is closed.
 */
export const useAuthStore = create<AuthStore>((set) => ({
  accessToken: initialParsed ? storedToken : null,
  role: initialParsed?.role ?? null,
  userId: initialParsed?.userId ?? null,

  setToken: (token: string) => {
    const parsed = parseToken(token);
    if (!parsed) return;
    sessionStorage.setItem("accessToken", token);
    set({ accessToken: token, role: parsed.role, userId: parsed.userId });
  },

  clearAuth: () => {
    sessionStorage.removeItem("accessToken");
    set({ accessToken: null, role: null, userId: null });
  },
}));
