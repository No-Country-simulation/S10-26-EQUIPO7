import axios from "axios";

export const TOKEN_STORAGE_KEY = "repusense.accessToken";
export const USER_STORAGE_KEY = "repusense.user";
export const SESSION_EXPIRED_EVENT = "repusense:session-expired";

export const api = axios.create({
  baseURL: (import.meta.env.VITE_API_URL || "http://localhost:8080")
    .replace(/\/$/, ""),
});

export function saveSession(token, user) {
  localStorage.setItem(TOKEN_STORAGE_KEY, token);
  localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
  return getTokenExpiration(token);
}

export function clearSession() {
  localStorage.removeItem(TOKEN_STORAGE_KEY);
  localStorage.removeItem(USER_STORAGE_KEY);
}

export function getTokenExpiration(token) {
  const payload = JSON.parse(
    atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/"))
  );
  return typeof payload.exp === "number" ? payload.exp * 1000 : null;
}

export function getStoredSession() {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY);
  const storedUser = localStorage.getItem(USER_STORAGE_KEY);

  if (!token || !storedUser) {
    clearSession();
    return null;
  }

  try {
    const expiresAt = getTokenExpiration(token);
    const user = JSON.parse(storedUser);

    if (
      expiresAt === null ||
      expiresAt <= Date.now() ||
      typeof user.id !== "number" ||
      typeof user.name !== "string" ||
      typeof user.email !== "string"
    ) {
      clearSession();
      return null;
    }

    return { token, user, expiresAt };
  } catch {
    clearSession();
    return null;
  }
}

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isAuthRequest = String(error.config?.url || "").includes("/api/auth/");
    if (
      error.response?.status === 401 &&
      !isAuthRequest &&
      localStorage.getItem(TOKEN_STORAGE_KEY)
    ) {
      clearSession();
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
    }
    return Promise.reject(error);
  }
);
