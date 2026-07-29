import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api, ApiError } from "../api/client";
import type { ResultadoOptimizacion } from "../types";
import { ErrorMessage } from "../components/ErrorMessage";

export function ResultadoPage() {
  const { id } = useParams<{ id: string }>();
  const [resultado, setResultado] = useState<ResultadoOptimizacion | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    api
      .get<ResultadoOptimizacion>(`/resultados/${id}`)
      .then(setResultado)
      .catch((err) => setError(err instanceof ApiError ? err.message : "No se pudo cargar el resultado"));
  }, [id]);

  if (error) {
    return <ErrorMessage mensaje={error} />;
  }

  if (!resultado) {
    return <p>Cargando...</p>;
  }

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <Link to={`/escenarios/${resultado.escenarioId}`}>← Escenario</Link>
          <h1>Resultado de la optimizacion</h1>
        </div>
      </header>

      <div className="resumen-resultado">
        <div>
          <span className="metrica">{resultado.retrasoTotalMinutos}</span>
          <span>minutos de retraso total</span>
        </div>
        <div>
          <span className="metrica">{resultado.retrasoMaximoMinutos}</span>
          <span>retraso maximo individual</span>
        </div>
      </div>

      <table className="tabla-resultado">
        <thead>
          <tr>
            <th>Orden</th>
            <th>Cofradia</th>
            <th>Entrada al recorrido oficial</th>
            <th>Salida del recorrido oficial</th>
            <th>Hora oficial publicada</th>
            <th>Retraso</th>
          </tr>
        </thead>
        <tbody>
          {resultado.cofradias.map((cofradia) => (
            <tr key={cofradia.cofradiaId} className={cofradia.retrasoMinutos > 0 ? "con-retraso" : ""}>
              <td>{cofradia.ordenAsignado}</td>
              <td>{cofradia.nombreCofradia}</td>
              <td>{cofradia.horaEntradaCalculada}</td>
              <td>{cofradia.horaSalidaCalculada}</td>
              <td>{cofradia.horaOficialPaso ?? "—"}</td>
              <td>{cofradia.retrasoMinutos} min</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
