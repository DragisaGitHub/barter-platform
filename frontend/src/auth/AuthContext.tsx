import {
  createContext,
  useContext,
  useReducer,
  useEffect,
  type ReactNode,
} from "react";
import { apiClient } from "../api/axios";
import { tokenService } from "./token.service";
import i18n from "@/i18n";
import {
  DEFAULT_LANGUAGE,
  mapBackendToFrontendLanguage,
} from "@/i18n/languageMapping";
import type {
  CurrentUserResponse,
  LoginRequest,
  RegisterRequest,
  TokenResponse,
} from "../api/generated/types";

interface AuthState {
  user: CurrentUserResponse | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}

type AuthAction =
  | { type: "SET_USER"; payload: CurrentUserResponse }
  | { type: "LOGOUT" }
  | { type: "SET_LOADING"; payload: boolean };

const authReducer = (state: AuthState, action: AuthAction): AuthState => {
  switch (action.type) {
    case "SET_USER":
      return { user: action.payload, isAuthenticated: true, isLoading: false };
    case "LOGOUT":
      return { user: null, isAuthenticated: false, isLoading: false };
    case "SET_LOADING":
      return { ...state, isLoading: action.payload };
    default:
      return state;
  }
};

interface AuthContextValue extends AuthState {
  login: (credentials: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
  replaceUser: (user: CurrentUserResponse) => Promise<void>;
  hasRole: (role: string) => boolean;
  hasPermission: (permission: string) => boolean;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(authReducer, {
    user: null,
    isAuthenticated: false,
    isLoading: true,
  });

  const syncLanguageWithUser = async (user: CurrentUserResponse | null) => {
    const language = user
      ? mapBackendToFrontendLanguage(user.preferredLanguage)
      : DEFAULT_LANGUAGE;

    if (i18n.language !== language) {
      await i18n.changeLanguage(language);
    }
  };

  const replaceUser = async (user: CurrentUserResponse) => {
    await syncLanguageWithUser(user);
    dispatch({ type: "SET_USER", payload: user });
  };

  useEffect(() => {
    const initAuth = async () => {
      if (!tokenService.hasTokens()) {
        await syncLanguageWithUser(null);
        dispatch({ type: "SET_LOADING", payload: false });
        return;
      }

      try {
        const response = await apiClient.get<CurrentUserResponse>("/auth/me");
        await replaceUser(response.data);
      } catch (error) {
        tokenService.clearTokens();
        await syncLanguageWithUser(null);
        dispatch({ type: "LOGOUT" });
      }
    };

    initAuth();
  }, []);

  const login = async (credentials: LoginRequest) => {
    const response = await apiClient.post<TokenResponse>(
      "/auth/login",
      credentials
    );
    const { accessToken, refreshToken, user } = response.data;
    tokenService.setTokens(accessToken, refreshToken);

    // TokenResponse includes the authenticated user directly
    if (user) {
      await replaceUser(user);
    } else {
      // Fallback: fetch user profile separately
      const userResponse = await apiClient.get<CurrentUserResponse>("/auth/me");
      await replaceUser(userResponse.data);
    }
  };

  const register = async (data: RegisterRequest) => {
    await apiClient.post<CurrentUserResponse>("/auth/register", data);
  };

  const logout = async () => {
    try {
      const refreshToken = tokenService.getRefreshToken();
      if (refreshToken) {
        await apiClient.post("/auth/logout", { refreshToken });
      }
    } catch (error) {
      // Continue logout even if API call fails
    } finally {
      tokenService.clearTokens();
      await syncLanguageWithUser(null);
      dispatch({ type: "LOGOUT" });
    }
  };

  const hasRole = (role: string): boolean => {
    return state.user?.roles?.some((r) => r.code === role) ?? false;
  };

  const hasPermission = (permission: string): boolean => {
    return state.user?.permissions?.some((p) => p.code === permission) ?? false;
  };

  return (
    <AuthContext.Provider
      value={{
        ...state,
        login,
        register,
        logout,
        replaceUser,
        hasRole,
        hasPermission,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
