# cofradia-scheduler — frontend

Interfaz web del proyecto [cofradia-scheduler](../README.md): permite registrarse, crear
escenarios (días de simulación), añadir cofradías, dibujar sus recorridos sobre un mapa y lanzar
la optimización, viendo el resultado en una tabla.

Para la documentación funcional completa (qué resuelve la app, flujo de uso paso a paso) y la
documentación técnica del backend, ver el [README principal](../README.md). Este documento cubre
solo el frontend.

## Stack

| Pieza | Tecnología |
|---|---|
| Framework | React 19 + TypeScript |
| Build / dev server | Vite |
| Enrutado | React Router (`react-router-dom`) |
| Mapa | Leaflet + `leaflet-draw` (dibujo de polilíneas a mano alzada) |
| Llamadas a la API | `fetch` nativo, sin librería adicional (ver `src/api/client.ts`) |
| Sesión | JWT guardado en `localStorage`, gestionado por `src/auth/AuthContext.tsx` |

No usa ningún framework de estilos (CSS plano en `src/index.css`) ni gestor de estado global
aparte del `AuthContext` — cada página carga sus propios datos con `useEffect` + `fetch`.

## Requisitos previos

- **Node.js 20 o superior** (LTS). Desarrollado con **Node 24.18.1 LTS**.
- El **backend corriendo en `http://localhost:8080`** (ver [README principal](../README.md#arranque-rápido)) — el frontend no funciona de forma aislada, necesita la API real.

## Arranque rápido

```bash
cd frontend
npm install
npm run dev
```

Abre `http://localhost:5173`. El backend debe estar arrancado y con CORS habilitado para ese
origen (ya configurado en `SecurityConfig` — ver más abajo).

Otros comandos:

```bash
npm run build     # compila TypeScript y genera el build de produccion en dist/
npm run preview   # sirve el build de produccion localmente, para probarlo
npm run lint      # oxlint
```

## Estructura

```
frontend/src/
├── main.tsx              # punto de entrada: BrowserRouter + AuthProvider
├── App.tsx                # definicion de rutas
├── types.ts                # tipos TypeScript, uno a uno con los DTOs del backend
├── api/
│   └── client.ts            # wrapper de fetch: URL base, cabecera Authorization, manejo de errores
├── auth/
│   └── AuthContext.tsx        # sesion JWT: login, registro, logout, persistencia en localStorage
├── components/
│   ├── RequireAuth.tsx          # redirige a /login si no hay sesion
│   ├── ErrorMessage.tsx          # aviso de error reutilizable
│   └── MapaRecorrido.tsx          # mapa Leaflet + control de dibujo (leaflet-draw)
└── pages/
    ├── LoginPage.tsx
    ├── RegisterPage.tsx
    ├── EscenariosPage.tsx          # "/" — listar y crear escenarios del usuario
    ├── EscenarioDetailPage.tsx      # "/escenarios/:id" — cofradias del escenario + optimizar
    ├── CofradiaDetailPage.tsx        # "/cofradias/:id" — dibujar y listar recorridos
    └── ResultadoPage.tsx              # "/resultados/:id" — tabla de resultado de optimizacion
```

## Rutas

| Ruta | Página | Protegida |
|---|---|---|
| `/login` | Iniciar sesión | No |
| `/register` | Crear cuenta | No |
| `/` | Mis escenarios (listar/crear) | Sí |
| `/escenarios/:id` | Cofradías del escenario, botón "Optimizar" | Sí |
| `/cofradias/:id` | Mapa para dibujar recorridos (IDA/OFICIAL/VUELTA) | Sí |
| `/resultados/:id` | Tabla con el resultado de una optimización | Sí |

Las rutas protegidas están envueltas en `<RequireAuth>`, que redirige a `/login` si no hay sesión
activa.

## Cómo se dibuja un recorrido

`MapaRecorrido` monta un mapa Leaflet centrado en Málaga con el control de dibujo de
`leaflet-draw` (solo la herramienta de polilínea está habilitada). Al terminar un trazo:

1. Se captura el evento `draw:created` de Leaflet.
2. Se extraen los puntos (`lat`/`lon`) del polígono dibujado y se calcula la distancia sumando
   `LatLng.distanceTo` entre puntos consecutivos.
3. Se muestra un botón de confirmación ("Guardar como IDA/OFICIAL/VUELTA") — el trazo no se
   guarda hasta que el usuario lo confirma.
4. Al confirmar, se hace `POST /api/cofradias/{id}/recorridos` con los puntos y la distancia, y
   se recarga la lista de tramos guardados (que se redibujan sobre el mapa con un color distinto
   por tipo).

## Conexión con el backend

- La URL base de la API está fijada en [`src/api/client.ts`](src/api/client.ts) como
  `http://localhost:8080/api` — no hay todavía variables de entorno (`.env`) para configurarla;
  si se despliega en otro sitio, hay que cambiar esa constante a mano.
- El backend expone CORS solo para `http://localhost:5173` (ver
  `corsConfigurationSource()` en
  [`SecurityConfig.java`](../src/main/java/com/cofradias/config/SecurityConfig.java) del
  backend) — si cambias el puerto del dev server de Vite, tienes que actualizar esa lista también.
- El token JWT se guarda en `localStorage` bajo la clave `cofradia-scheduler.sesion` y se adjunta
  automáticamente a cada petición vía `Authorization: Bearer <token>`.

## Estado y limitaciones conocidas

- Sin tests automatizados todavía (el backend sí tiene una suite completa, ver README principal).
- La URL de la API está hardcodeada (ver arriba) — para producción haría falta una variable de
  entorno de Vite (`VITE_API_URL` o similar).
- No hay edición ni borrado de escenarios/cofradías/recorridos desde la interfaz, solo alta y
  consulta (el backend tampoco expone esos endpoints todavía).
- El mapa no valida que el tramo `OFICIAL` sea el mismo para todas las cofradías de un escenario
  (cada una dibuja el suyo) — es responsabilidad del usuario ser consistente al calcarlo.
