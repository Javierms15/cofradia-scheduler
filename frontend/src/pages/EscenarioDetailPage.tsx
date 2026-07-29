import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api, ApiError } from "../api/client";
import type { Cofradia, Escenario, ResultadoResumen } from "../types";
import { ErrorMessage } from "../components/ErrorMessage";

const CAMPOS_INICIALES = {
  nombre: "",
  numNazarenos: "",
  longitudCortejoMetros: "",
  velocidadMarchaMetrosMinuto: "",
  horaSalidaEstimada: "",
  horaEncierroLimite: "",
  horaOficialPaso: "",
};

export function EscenarioDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [escenario, setEscenario] = useState<Escenario | null>(null);
  const [cofradias, setCofradias] = useState<Cofradia[]>([]);
  const [resultados, setResultados] = useState<ResultadoResumen[]>([]);
  const [campos, setCampos] = useState(CAMPOS_INICIALES);
  const [error, setError] = useState<string | null>(null);
  const [optimizando, setOptimizando] = useState(false);

  async function cargarTodo() {
    if (!id) return;
    const [escenarioData, cofradiasData, resultadosData] = await Promise.all([
      api.get<Escenario>(`/escenarios/${id}`),
      api.get<Cofradia[]>(`/escenarios/${id}/cofradias`),
      api.get<ResultadoResumen[]>(`/escenarios/${id}/resultados`),
    ]);
    setEscenario(escenarioData);
    setCofradias(cofradiasData);
    setResultados(resultadosData);
  }

  useEffect(() => {
    cargarTodo().catch((err) => setError(err instanceof ApiError ? err.message : "No se pudo cargar el escenario"));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  function actualizarCampo(campo: keyof typeof CAMPOS_INICIALES, valor: string) {
    setCampos((anterior) => ({ ...anterior, [campo]: valor }));
  }

  async function handleCrearCofradia(event: FormEvent) {
    event.preventDefault();
    if (!id) return;
    setError(null);
    try {
      await api.post(`/escenarios/${id}/cofradias`, {
        nombre: campos.nombre,
        numNazarenos: campos.numNazarenos ? Number(campos.numNazarenos) : null,
        longitudCortejoMetros: campos.longitudCortejoMetros ? Number(campos.longitudCortejoMetros) : null,
        velocidadMarchaMetrosMinuto: campos.velocidadMarchaMetrosMinuto
          ? Number(campos.velocidadMarchaMetrosMinuto)
          : null,
        horaSalidaEstimada: campos.horaSalidaEstimada ? `${campos.horaSalidaEstimada}:00` : null,
        horaEncierroLimite: campos.horaEncierroLimite ? `${campos.horaEncierroLimite}:00` : null,
        horaOficialPaso: campos.horaOficialPaso ? `${campos.horaOficialPaso}:00` : null,
      });
      setCampos(CAMPOS_INICIALES);
      await cargarTodo();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "No se pudo crear la cofradia");
    }
  }

  async function handleOptimizar() {
    if (!id) return;
    setError(null);
    setOptimizando(true);
    try {
      const resultado = await api.post<{ id: number }>(`/escenarios/${id}/optimizar`);
      navigate(`/resultados/${resultado.id}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "No se pudo optimizar el escenario");
    } finally {
      setOptimizando(false);
    }
  }

  if (!escenario) {
    return <p>Cargando...</p>;
  }

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <Link to="/">← Mis escenarios</Link>
          <h1>{escenario.nombre}</h1>
        </div>
        <button onClick={handleOptimizar} disabled={optimizando || cofradias.length === 0}>
          {optimizando ? "Optimizando..." : "Optimizar recorrido"}
        </button>
      </header>

      <ErrorMessage mensaje={error} />

      <section>
        <h2>Cofradias</h2>
        {cofradias.length === 0 ? (
          <p>Todavia no hay cofradias en este escenario.</p>
        ) : (
          <ul className="lista">
            {cofradias.map((cofradia) => (
              <li key={cofradia.id}>
                <Link to={`/cofradias/${cofradia.id}`}>{cofradia.nombre}</Link>
                {cofradia.horaOficialPaso ? ` — hora oficial: ${cofradia.horaOficialPaso}` : ""}
              </li>
            ))}
          </ul>
        )}

        <form onSubmit={handleCrearCofradia} className="form-grid">
          <input
            placeholder="Nombre"
            value={campos.nombre}
            onChange={(e) => actualizarCampo("nombre", e.target.value)}
            required
          />
          <input
            placeholder="Nazarenos"
            type="number"
            value={campos.numNazarenos}
            onChange={(e) => actualizarCampo("numNazarenos", e.target.value)}
          />
          <input
            placeholder="Longitud cortejo (m)"
            type="number"
            value={campos.longitudCortejoMetros}
            onChange={(e) => actualizarCampo("longitudCortejoMetros", e.target.value)}
          />
          <input
            placeholder="Velocidad (m/min)"
            type="number"
            value={campos.velocidadMarchaMetrosMinuto}
            onChange={(e) => actualizarCampo("velocidadMarchaMetrosMinuto", e.target.value)}
          />
          <label>
            Salida estimada
            <input
              type="time"
              value={campos.horaSalidaEstimada}
              onChange={(e) => actualizarCampo("horaSalidaEstimada", e.target.value)}
            />
          </label>
          <label>
            Encierro limite
            <input
              type="time"
              value={campos.horaEncierroLimite}
              onChange={(e) => actualizarCampo("horaEncierroLimite", e.target.value)}
            />
          </label>
          <label>
            Hora oficial de paso
            <input
              type="time"
              value={campos.horaOficialPaso}
              onChange={(e) => actualizarCampo("horaOficialPaso", e.target.value)}
            />
          </label>
          <button type="submit">Anadir cofradia</button>
        </form>
      </section>

      <section>
        <h2>Resultados de optimizacion</h2>
        {resultados.length === 0 ? (
          <p>Todavia no se ha optimizado este escenario.</p>
        ) : (
          <ul className="lista">
            {resultados.map((resultado) => (
              <li key={resultado.id}>
                <Link to={`/resultados/${resultado.id}`}>
                  {new Date(resultado.fechaCalculo).toLocaleString()} — retraso total{" "}
                  {resultado.retrasoTotalMinutos} min, maximo {resultado.retrasoMaximoMinutos} min
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
