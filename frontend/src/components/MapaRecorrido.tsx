import { useEffect, useRef } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import "leaflet-draw";
import "leaflet-draw/dist/leaflet.draw.css";
import type { Punto, Recorrido, TipoTramo } from "../types";

// Coordenadas por defecto: centro de Malaga (Calle Larios)
const CENTRO_MALAGA: [number, number] = [36.7213, -4.4214];

const COLOR_POR_TIPO: Record<TipoTramo, string> = {
  IDA: "#2563eb",
  OFICIAL: "#dc2626",
  VUELTA: "#16a34a",
};

interface MapaRecorridoProps {
  tipoSeleccionado: TipoTramo;
  recorridos: Recorrido[];
  onTrazoCompletado: (puntos: Punto[], distanciaMetros: number) => void;
}

export function MapaRecorrido({ tipoSeleccionado, recorridos, onTrazoCompletado }: MapaRecorridoProps) {
  const contenedorRef = useRef<HTMLDivElement>(null);
  const mapaRef = useRef<L.Map | null>(null);
  const capaExistentesRef = useRef<L.LayerGroup | null>(null);
  const capaDibujoRef = useRef<L.FeatureGroup | null>(null);
  const onTrazoCompletadoRef = useRef(onTrazoCompletado);
  onTrazoCompletadoRef.current = onTrazoCompletado;

  useEffect(() => {
    if (!contenedorRef.current || mapaRef.current) return;

    const mapa = L.map(contenedorRef.current).setView(CENTRO_MALAGA, 16);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: "© OpenStreetMap contributors",
      maxZoom: 19,
    }).addTo(mapa);

    const capaDibujo = new L.FeatureGroup();
    mapa.addLayer(capaDibujo);
    capaDibujoRef.current = capaDibujo;

    const capaExistentes = L.layerGroup().addTo(mapa);
    capaExistentesRef.current = capaExistentes;

    const control = new L.Control.Draw({
      draw: {
        polyline: { shapeOptions: { color: "#f97316", weight: 4 } },
        polygon: false,
        rectangle: false,
        circle: false,
        circlemarker: false,
        marker: false,
      },
      edit: { featureGroup: capaDibujo, remove: true },
    });
    mapa.addControl(control);

    mapa.on(L.Draw.Event.CREATED, (event) => {
      const layer = (event as L.DrawEvents.Created).layer as L.Polyline;
      capaDibujo.clearLayers();
      capaDibujo.addLayer(layer);

      const latLngs = layer.getLatLngs() as L.LatLng[];
      const puntos: Punto[] = latLngs.map((ll) => ({ lat: ll.lat, lon: ll.lng }));
      let distancia = 0;
      for (let i = 1; i < latLngs.length; i++) {
        distancia += latLngs[i - 1].distanceTo(latLngs[i]);
      }
      onTrazoCompletadoRef.current(puntos, Math.round(distancia));
    });

    mapaRef.current = mapa;

    return () => {
      mapa.remove();
      mapaRef.current = null;
    };
  }, []);

  // Redibuja los recorridos ya guardados cada vez que cambian
  useEffect(() => {
    const capa = capaExistentesRef.current;
    if (!capa) return;
    capa.clearLayers();

    recorridos.forEach((recorrido) => {
      if (recorrido.puntos.length < 2) return;
      const latLngs = recorrido.puntos.map((p) => [p.lat, p.lon] as [number, number]);
      L.polyline(latLngs, { color: COLOR_POR_TIPO[recorrido.tipo], weight: 4, dashArray: "6 4" })
        .bindTooltip(`${recorrido.tipo} (${Math.round(recorrido.distanciaMetros ?? 0)} m)`)
        .addTo(capa);
    });
  }, [recorridos]);

  return (
    <div>
      <p>
        Dibuja el tramo <strong style={{ color: COLOR_POR_TIPO[tipoSeleccionado] }}>{tipoSeleccionado}</strong> con
        la herramienta de linea del mapa (icono de la izquierda) y guardalo con el boton de abajo.
      </p>
      <div ref={contenedorRef} className="mapa" />
    </div>
  );
}
