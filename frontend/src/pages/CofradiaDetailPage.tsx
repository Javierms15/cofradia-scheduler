import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api, ApiError } from "../api/client";
import type { Cofradia, Punto, Recorrido, TipoTramo } from "../types";
import { ErrorMessage } from "../components/ErrorMessage";
import { MapaRecorrido } from "../components/MapaRecorrido";

const TIPOS: TipoTramo[] = ["IDA", "OFICIAL", "VUELTA"];

interface TrazoPendiente {
  puntos: Punto[];
  distanciaMetros: number;
}

export function CofradiaDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [cofradia, setCofradia] = useState<Cofradia | null>(null);
  const [recorridos, setRecorridos] = useState<Recorrido[]>([]);
  const [tipoSeleccionado, setTipoSeleccionado] = useState<TipoTramo>("OFICIAL");
  const [trazoPendiente, setTrazoPendiente] = useState<TrazoPendiente | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    if (!id) return;
    Promise.all([api.get<Cofradia>(`/cofradias/${id}`), api.get<Recorrido[]>(`/cofradias/${id}/recorridos`)])
      .then(([cofradiaData, recorridosData]) => {
        setCofradia(cofradiaData);
        setRecorridos(recorridosData);
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : "No se pudo cargar la cofradia"));
  }, [id]);

  function handleTrazoCompletado(puntos: Punto[], distanciaMetros: number) {
    setTrazoPendiente({ puntos, distanciaMetros });
  }

  async function handleGuardarTrazo() {
    if (!id || !trazoPendiente) return;
    setError(null);
    setGuardando(true);
    try {
      await api.post<Recorrido>(`/cofradias/${id}/recorridos`, {
        tipo: tipoSeleccionado,
        puntos: trazoPendiente.puntos,
        distanciaMetros: trazoPendiente.distanciaMetros,
      });
      setTrazoPendiente(null);
      const actualizados = await api.get<Recorrido[]>(`/cofradias/${id}/recorridos`);
      setRecorridos(actualizados);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "No se pudo guardar el recorrido");
    } finally {
      setGuardando(false);
    }
  }

  return (
    <div className="page">
      <header className="page-header">
        <div>
          {cofradia ? <Link to={`/escenarios/${cofradia.escenarioId}`}>← Escenario</Link> : null}
          <h1>Recorridos de la cofradia</h1>
        </div>
      </header>

      <ErrorMessage mensaje={error} />

      <div className="tipo-selector">
        {TIPOS.map((tipo) => (
          <label key={tipo}>
            <input
              type="radio"
              name="tipo"
              checked={tipoSeleccionado === tipo}
              onChange={() => setTipoSeleccionado(tipo)}
            />
            {tipo}
          </label>
        ))}
      </div>

      <MapaRecorrido tipoSeleccionado={tipoSeleccionado} recorridos={recorridos} onTrazoCompletado={handleTrazoCompletado} />

      {trazoPendiente && (
        <div className="trazo-pendiente">
          <p>
            Trazo listo: {trazoPendiente.puntos.length} puntos, {trazoPendiente.distanciaMetros} m.
          </p>
          <button onClick={handleGuardarTrazo} disabled={guardando}>
            {guardando ? "Guardando..." : `Guardar como ${tipoSeleccionado}`}
          </button>
        </div>
      )}

      <section>
        <h2>Tramos guardados</h2>
        {recorridos.length === 0 ? (
          <p>Todavia no hay ningun tramo dibujado.</p>
        ) : (
          <ul className="lista">
            {recorridos.map((recorrido) => (
              <li key={recorrido.id}>
                {recorrido.tipo} — {Math.round(recorrido.distanciaMetros ?? 0)} m ({recorrido.puntos.length} puntos)
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
