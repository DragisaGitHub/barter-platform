import axios from "axios";
import type { TokenResponse } from "./generated/types";
import { tokenService } from "../auth/token.service";

const baseURL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api/v1";

export const apiClient = axios.create({
  baseURL,
  headers: {
    "Content-Type": "application/json",
  },
});

let isRefreshing = false;
let refreshSubscribers: ((token: string) => void)[] = [];

function onRefreshed(token: string) {
  refreshSubscribers.forEach((callback) => callback(token));
  refreshSubscribers = [];
}

function addRefreshSubscriber(callback: (token: string) => void) {
  refreshSubscribers.push(callback);
}

apiClient.interceptors.request.use(
  (config) => {
    const token = tokenService.getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const requestUrl = typeof originalRequest?.url === "string" ? originalRequest.url : "";
    const isAuthMeRequest = requestUrl.includes("/auth/me");

    if (requestUrl.includes("/auth/login")) {
      return Promise.reject(error);
    }

    if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve) => {
          addRefreshSubscriber((token: string) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            resolve(apiClient(originalRequest));
          });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const refreshToken = tokenService.getRefreshToken();
        if (!refreshToken) {
          tokenService.clearTokens();

          if (isAuthMeRequest) {
            isRefreshing = false;
            return Promise.reject(error);
          }

          throw new Error("No refresh token");
        }

        const response = await axios.post<TokenResponse>(
          `${baseURL}/auth/refresh`,
          { refreshToken }
        );

        const { accessToken, refreshToken: newRefreshToken } = response.data;
        tokenService.setTokens(accessToken, newRefreshToken);

        apiClient.defaults.headers.common.Authorization = `Bearer ${accessToken}`;
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;

        onRefreshed(accessToken);
        isRefreshing = false;

        return apiClient(originalRequest);
      } catch (refreshError) {
        isRefreshing = false;
        tokenService.clearTokens();

        if (!isAuthMeRequest) {
          window.location.href = "/login";
          return Promise.reject(refreshError);
        }

        return Promise.reject(error);
      }
    }

    return Promise.reject(error);
  }
);
