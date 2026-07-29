export interface Usuario {
  id: number;
  nombre: string;
  email: string;
  fechaCreacion: string;
}

export interface TokenResponse {
  token: string;
  usuario: Usuario;
}

export interface Escenario {
  id: number;
  nombre: string;
  fecha: string | null;
  fechaCreacion: string;
  usuarioId: number;
}

export type TipoTramo = "IDA" | "OFICIAL" | "VUELTA";

export interface Cofradia {
  id: number;
  nombre: string;
  numNazarenos: number | null;
  longitudCortejoMetros: number | null;
  velocidadMarchaMetrosMinuto: number | null;
  horaSalidaEstimada: string | null;
  horaEncierroLimite: string | null;
  horaOficialPaso: string | null;
  escenarioId: number;
}

export interface Punto {
  lat: number;
  lon: number;
}

export interface Recorrido {
  id: number;
  tipo: TipoTramo;
  puntos: Punto[];
  distanciaMetros: number | null;
  cofradiaId: number;
}

export interface CofradiaResultado {
  cofradiaId: number;
  nombreCofradia: string;
  ordenAsignado: number;
  horaEntradaCalculada: string;
  horaSalidaCalculada: string;
  horaOficialPaso: string | null;
  retrasoMinutos: number;
}

export interface ResultadoOptimizacion {
  id: number;
  fechaCalculo: string;
  retrasoTotalMinutos: number;
  retrasoMaximoMinutos: number;
  escenarioId: number;
  cofradias: CofradiaResultado[];
}

export interface ResultadoResumen {
  id: number;
  fechaCalculo: string;
  retrasoTotalMinutos: number;
  retrasoMaximoMinutos: number;
}

export interface ProblemDetail {
  title?: string;
  status?: number;
  detail?: string;
  errores?: Record<string, string>;
}
