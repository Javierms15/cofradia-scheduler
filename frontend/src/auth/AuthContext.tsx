import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { api, setAuthToken } from "../api/client";
import type { TokenResponse, Usuario } from "../types";

interface AuthContextValue {
  usuario: Usuario | null;
  cargando: boolean;
  login: (email: string, password: string) => Promise<void>;
  registrar: (nombre: string, email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const STORAGE_KEY = "cofradia-scheduler.sesion";

interface SesionGuardada {
  token: string;
  usuario: Usuario;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null);
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    const guardada = localStorage.getItem(STORAGE_KEY);
    if (guardada) {
      const sesion: SesionGuardada = JSON.parse(guardada);
      setAuthToken(sesion.token);
      setUsuario(sesion.usuario);
    }
    setCargando(false);
  }, []);

  function guardarSesion(respuesta: TokenResponse) {
    setAuthToken(respuesta.token);
    setUsuario(respuesta.usuario);
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ token: respuesta.token, usuario: respuesta.usuario }));
  }

  async function login(email: string, password: string) {
    const respuesta = await api.post<TokenResponse>("/auth/login", { email, password });
    guardarSesion(respuesta);
  }

  async function registrar(nombre: string, email: string, password: string) {
    const respuesta = await api.post<TokenResponse>("/auth/register", { nombre, email, password });
    guardarSesion(respuesta);
  }

  function logout() {
    setAuthToken(null);
    setUsuario(null);
    localStorage.removeItem(STORAGE_KEY);
  }

  return (
    <AuthContext.Provider value={{ usuario, cargando, login, registrar, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth debe usarse dentro de AuthProvider");
  }
  return context;
}
