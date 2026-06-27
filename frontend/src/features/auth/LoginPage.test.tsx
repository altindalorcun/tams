import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ThemeProvider } from "next-themes";
import { LoginPage } from "./LoginPage";
import * as authApi from "@/api/authApi";
import * as authStore from "./authStore";

vi.mock("@/api/authApi");
vi.mock("./authStore");

function renderLogin() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <ThemeProvider attribute="class">
      <QueryClientProvider client={qc}>
        <MemoryRouter initialEntries={["/login"]}>
          <LoginPage />
        </MemoryRouter>
      </QueryClientProvider>
    </ThemeProvider>,
  );
}

describe("LoginPage", () => {
  const mockSetAuth = vi.fn();

  beforeEach(() => {
    vi.mocked(authStore.useAuthStore).mockReturnValue({
      accessToken: null,
      role: null,
      userId: null,
      username: null,
      mustChangePassword: false,
      setAuth: mockSetAuth,
      clearAuth: vi.fn(),
    });
  });

  it("renders email and password fields", () => {
    renderLogin();
    expect(screen.getByLabelText(/e-posta/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/şifre/i)).toBeInTheDocument();
  });

  it("shows validation errors when submitted empty", async () => {
    const user = userEvent.setup();
    renderLogin();
    await user.click(screen.getByRole("button", { name: /giriş yap/i }));
    await waitFor(() => {
      expect(screen.getByText(/e-posta gereklidir/i)).toBeInTheDocument();
    });
  });

  it("calls login API and sets auth on success", async () => {
    const user = userEvent.setup();
    vi.mocked(authApi.login).mockResolvedValueOnce({
      accessToken: "test-token",
      refreshToken: "refresh-token",
      username: "admin",
      mustChangePassword: false,
    });
    renderLogin();

    await user.type(screen.getByLabelText(/e-posta/i), "admin@hacettepe.edu.tr");
    await user.type(screen.getByLabelText(/şifre/i), "password123");
    await user.click(screen.getByRole("button", { name: /giriş yap/i }));

    await waitFor(() => {
      expect(authApi.login).toHaveBeenCalledWith({
        email: "admin@hacettepe.edu.tr",
        password: "password123",
      });
      expect(mockSetAuth).toHaveBeenCalledWith("test-token", false, "admin");
    });
  });

  it("shows inline error when login fails", async () => {
    const user = userEvent.setup();
    vi.mocked(authApi.login).mockRejectedValueOnce(new Error("Unauthorized"));
    renderLogin();

    await user.type(screen.getByLabelText(/e-posta/i), "wrong@example.com");
    await user.type(screen.getByLabelText(/şifre/i), "wrongpass");
    await user.click(screen.getByRole("button", { name: /giriş yap/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/e-posta veya şifre hatalı/i);
    });
  });
});
