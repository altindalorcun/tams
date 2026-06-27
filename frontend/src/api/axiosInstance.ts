import axios from "axios";

/** Login endpoint — 401 here means invalid credentials, not an expired session. */
const AUTH_LOGIN_PATH = "/api/v1/auth/login";

const API_URL = import.meta.env.VITE_API_URL as string | undefined;

if (!API_URL) {
  console.warn("VITE_API_URL is not set — falling back to http://localhost:8080");
}

/**
 * Shared Axios instance.
 * All API calls in the app must go through this instance.
 */
export const axiosInstance = axios.create({
  baseURL: API_URL ?? "http://localhost:8080",
  headers: { "Content-Type": "application/json" },
});

axiosInstance.interceptors.request.use((config) => {
  const token = sessionStorage.getItem("accessToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    const requestUrl = error.config?.url ?? "";
    const isLoginRequest = requestUrl.includes(AUTH_LOGIN_PATH);

    if (error.response?.status === 401 && !isLoginRequest) {
      sessionStorage.removeItem("accessToken");
      sessionStorage.removeItem("mustChangePassword");
      sessionStorage.removeItem("username");
      window.location.replace("/login");
    }
    return Promise.reject(error);
  },
);
