import axios from "axios";

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
    if (error.response?.status === 401) {
      sessionStorage.removeItem("accessToken");
      window.location.replace("/login");
    }
    return Promise.reject(error);
  },
);
