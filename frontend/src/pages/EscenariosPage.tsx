import { useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { api, ApiError } from "../api/client";
import type { Escenario } from "../types";
import { ErrorMessage } from "../components/ErrorMessage";

export function EscenariosPage() {
  const { usuario, logout } = useAuth();
  const [escenarios, setEscenarios] = useState<Escenario[]>([]);
  const [nombre, setNombre] = useState("");
  const [fecha, setFecha] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [cargando, setCargando] = useState(true);

  async function cargarEscenarios() {
    if (!usuario) return;
    setCargando(true);
    try {
      const datos = await api.get<Escenario[]>(`/usuarios/${usuario.id}/escenarios`);
      setEscenarios(datos);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "No se pudieron cargar los escenarios");
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    cargarEscenarios();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [usuario]);

  async function handleCrear(event: FormEvent) {
    event.preventDefault();
    if (!usuario) return;
    setError(null);
    try {
      await api.post(`/usuarios/${usuario.id}/escenarios`, { nombre, fecha: fecha || null });
      setNombre("");
      setFecha("");
      await cargarEscenarios();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "No se pudo crear el escenario");
    }
  }

  return (
    <div className="page">
      <header className="page-header">
        <h1>Mis escenarios</h1>
        <div>
          <span>{usuario?.nombre}</span>
          <button onClick={logout}>Cerrar sesion</button>
        </div>
      </header>

      <form onSubmit={handleCrear} className="inline-form">
        <input
          placeholder="Nombre del escenario (p. ej. Domingo de Ramos 2027)"
          value={nombre}
          onChange={(e) => setNombre(e.target.value)}
          required
        />
        <input type="date" value={fecha} onChange={(e) => setFecha(e.target.value)} />
        <button type="submit">Crear escenario</button>
      </form>
      <ErrorMessage mensaje={error} />

      {cargando ? (
        <p>Cargando...</p>
      ) : escenarios.length === 0 ? (
        <p>Todavia no tienes ningun escenario. Crea el primero arriba.</p>
      ) : (
        <ul className="lista">
          {escenarios.map((escenario) => (
            <li key={escenario.id}>
              <Link to={`/escenarios/${escenario.id}`}>
                {escenario.nombre} {escenario.fecha ? `— ${escenario.fecha}` : ""}
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
