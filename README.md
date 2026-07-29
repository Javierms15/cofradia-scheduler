# cofradia-scheduler

Aplicación web para calcular, por optimización, el mejor orden e itinerarios de las cofradías
de un día de la Semana Santa de Málaga, evitando retrasos en el recorrido oficial.

Este documento reúne dos guías independientes:

- **[Documentación funcional](#documentación-funcional)** — qué problema resuelve la aplicación,
  quién la usa y cómo se usa, paso a paso, sin entrar en detalles de implementación.
- **[Documentación técnica](#documentación-técnica)** — arquitectura, cómo poner en marcha el
  proyecto, modelo de datos, API REST completa, cómo funciona el motor de optimización por dentro,
  y cómo ejecutar los tests.

---

# Documentación funcional

## El problema

El recorrido oficial (la "carrera oficial") es un conjunto fijo de calles por el que pasan,
en fila, todas las cofradías del día. Es análogo a programar trenes en una vía única: solo puede
haber una cofradía a la vez, en un único orden, sin poder adelantarse entre sí. Si una cofradía
alcanza a la que va delante, se produce un parón que se propaga en cascada a las que vienen detrás.

El Ayuntamiento y las cofradías publican un horario oficial de paso por el recorrido, pero ese
horario ideal no siempre es compatible con las velocidades de marcha, la longitud de cada cortejo
y las horas de salida/encierro reales. El objetivo de la aplicación es, dado un conjunto de
cofradías con sus datos reales, encontrar **el orden de entrada al recorrido oficial y los
horarios** que minimicen los retrasos respecto al horario oficial publicado.

## ¿Quién la usa?

Cualquier persona que quiera simular o planificar un día de la Semana Santa malagueña: aficionados
que quieren anticipar retrasos, o quien organice/analice el itinerario oficial. Cada usuario
gestiona sus propios escenarios de forma privada — nadie puede ver ni modificar los datos de otro
usuario.

## Conceptos clave

| Concepto | Significado |
|---|---|
| **Escenario** | Un "día" de simulación (p. ej. "Domingo de Ramos 2027"), con su fecha y su conjunto de cofradías. |
| **Cofradía** | Una hermandad participante ese día: número de nazarenos, longitud del cortejo, velocidad de marcha, hora de salida estimada, hora límite de encierro y hora oficial de paso publicada. |
| **Recorrido** | Un tramo del itinerario de una cofradía, dibujado como una línea de puntos geográficos. Hay tres tipos: **IDA** (de la iglesia al recorrido oficial), **OFICIAL** (el tramo compartido por todas las cofradías, sin adelantamientos) y **VUELTA** (del recorrido oficial de vuelta a la iglesia). |
| **Optimización** | El cálculo que, para un escenario completo, decide el orden de paso y los horarios reales de cada cofradía por el recorrido oficial, minimizando los retrasos. |
| **Resultado** | El resultado guardado de una optimización: retraso total, retraso máximo, y el detalle por cofradía (orden asignado, hora de entrada/salida calculada, retraso en minutos). |

## Flujo de uso paso a paso

1. **Registro** — se crea una cuenta con nombre, email y contraseña. La respuesta incluye un token
   de sesión (JWT) que hay que enviar en todas las peticiones siguientes.
2. **Crear un escenario** — se define el día a simular (nombre y fecha).
3. **Añadir las cofradías** del escenario, con sus datos: número de nazarenos, longitud del
   cortejo, velocidad de marcha, hora de salida estimada, hora de encierro límite y hora oficial
   de paso.
4. **Dibujar los recorridos** de cada cofradía: como mínimo el tramo OFICIAL es obligatorio para
   poder optimizar (es el tramo compartido); los tramos IDA y VUELTA son opcionales — si no se
   indican, se asume que la cofradía ya está en la entrada del recorrido oficial a su hora de
   salida, y que vuelve a su iglesia instantáneamente al salir.
5. **Lanzar la optimización** del escenario. La aplicación calcula el orden óptimo de paso y
   los horarios reales de cada cofradía.
6. **Consultar el resultado**: por cada cofradía se obtiene el orden asignado, la hora de entrada
   y salida calculada del recorrido oficial, y el retraso en minutos respecto a su hora oficial
   publicada. También se puede volver a consultar cualquier resultado anterior de un escenario.

## Cómo interpretar el resultado

- **`retrasoTotalMinutos`**: la suma de los minutos de retraso de todas las cofradías. Es el valor
  que el motor de optimización minimiza.
- **`retrasoMaximoMinutos`**: el mayor retraso individual entre todas las cofradías del escenario.
- Por cada cofradía, **`retrasoMinutos`** es `0` si consigue pasar por el recorrido oficial a su
  hora publicada o antes; solo cuenta como retraso el tiempo que pasa **después** de la hora
  oficial. Llegar antes de la hora publicada no penaliza.
- **`ordenAsignado`** indica en qué posición de la fila entra cada cofradía al recorrido oficial
  (1 = la primera).

### Ejemplo: margen suficiente → sin retrasos

Tres cofradías con horas de salida holgadas y horas oficiales de paso separadas varios minutos:
el resultado es `retrasoTotalMinutos: 0` — todas caben sin pisarse.

### Ejemplo: conflicto real → retraso en cascada

Tres cofradías idénticas, todas con la misma hora de salida y la misma hora oficial de paso
(compitiendo por el mismo hueco), cada una necesitando 17 minutos para liberar el recorrido
oficial (tránsito + longitud del cortejo): el resultado es que la primera entra sin retraso, la
segunda absorbe 12 minutos de retraso y la tercera 29 minutos (`retrasoTotalMinutos: 41`,
`retrasoMaximoMinutos: 29`) — exactamente el efecto de "parón en cascada" que describe el
problema.

---

# Documentación técnica

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Backend | Spring Boot 4.1.0 (Java 21) |
| Persistencia | Spring Data JPA + Hibernate 7 |
| Base de datos | PostgreSQL 17 + PostGIS 3.5 (geometrías de los recorridos) |
| Mapeo espacial | Hibernate Spatial + JTS (`org.locationtech.jts.geom.LineString`) |
| Motor de optimización | [Choco Solver](https://choco-solver.org/) 4.10.18 (CP solver puro en Java, sin dependencias nativas) |
| Autenticación | Spring Security + JWT (`io.jsonwebtoken`, jjwt 0.12.6) |
| Build | Maven (con Maven Wrapper, `mvnw`/`mvnw.cmd`) |
| Tests | JUnit 5, Mockito, AssertJ, MockMvc, JaCoCo |
| Frontend | React 19 + TypeScript + Vite, React Router, Leaflet + `leaflet-draw` |

El frontend vive en [`frontend/`](frontend) y tiene su propio
**[README con la documentación técnica del frontend](frontend/README.md)** (estructura, rutas,
cómo se dibuja un recorrido en el mapa, conexión con la API). Este documento cubre el backend en
detalle y, más abajo, el arranque conjunto de todo el proyecto.

## Requisitos previos

- **JDK 21 o superior** — se recomienda una versión **LTS** (21 o 25). El proyecto se desarrolló
  con **JDK 25 (Temurin)**. *Evitar versiones no-LTS recién publicadas*: durante el desarrollo,
  Lombok no generaba código en JDK 26 sin lanzar ningún error (ver
  [incidencias de entorno](#incidencias-de-entorno-encontradas-durante-el-desarrollo)).
- **Docker Desktop** — para levantar PostgreSQL + PostGIS vía `docker-compose.yml`. En Windows
  requiere WSL2.
- **Node.js 20 o superior** (LTS) — solo para el frontend. Desarrollado con Node 24.18.1 LTS.
- No hace falta instalar Maven: el proyecto incluye el Maven Wrapper (`mvnw` / `mvnw.cmd`).

## Arranque rápido (todo el proyecto)

Se necesitan **tres terminales** abiertas a la vez: base de datos, backend y frontend.

```bash
# 1. Base de datos — PostgreSQL 17 + PostGIS 3.5 (queda en segundo plano)
docker compose up -d

# 2. Backend — API REST en http://localhost:8080
./mvnw spring-boot:run
```

```bash
# 3. Frontend — en otra terminal, interfaz web en http://localhost:5173
cd frontend
npm install    # solo la primera vez
npm run dev
```

Abre **`http://localhost:5173`** en el navegador — ahí está la aplicación completa (registro,
login, escenarios, cofradías, mapa para dibujar recorridos y optimización). El backend por sí
solo, en `http://localhost:8080`, solo expone la API REST (ver más abajo) sin interfaz visual.

La primera vez que arranca el backend, Hibernate crea el esquema completo de la base de datos
automáticamente (`spring.jpa.hibernate.ddl-auto=update` en
[application.yml](src/main/resources/application.yml)) — no hace falta ejecutar ningún script SQL
a mano.

Credenciales de la base de datos de desarrollo (ver [docker-compose.yml](docker-compose.yml)):
base de datos `cofradia_scheduler`, usuario y contraseña `cofradia`.

Para parar todo: `Ctrl+C` en las terminales del backend y frontend, y `docker compose down` para
la base de datos (añade `-v` si además quieres borrar los datos guardados).

Más detalle del frontend (estructura, rutas, cómo funciona el mapa) en su
[propio README](frontend/README.md).

## Estructura del proyecto

```
src/main/java/com/cofradias/
├── CofradiaSchedulerApplication.java   # clase de arranque de Spring Boot
├── model/          # entidades JPA (Usuario, Escenario, Cofradia, Recorrido, ...)
├── repository/      # interfaces Spring Data JPA
├── dto/             # records de entrada/salida de la API (uno por agregado)
├── web/             # controladores REST + manejador global de excepciones
├── security/         # JWT (emisión/validacion), filtro de autenticacion, UserDetailsService
├── config/           # configuracion de Spring Security
└── optimization/      # motor de optimizacion con Choco Solver
```

## Modelo de datos

```
Usuario 1───N Escenario 1───N Cofradia 1───N Recorrido
                    │                │
                    │                └──N ResultadoCofradia N──1 ResultadoOptimizacion
                    └──────────────────────────────N ResultadoOptimizacion
```

- **`Usuario`** — cuenta registrada (nombre, email único, hash de contraseña con BCrypt).
- **`Escenario`** — un día de simulación, pertenece a un `Usuario`.
- **`Cofradia`** — pertenece a un `Escenario`: nombre, número de nazarenos, longitud del cortejo
  (metros), velocidad de marcha (metros/minuto), hora de salida estimada, hora de encierro límite
  y hora oficial de paso publicada.
- **`Recorrido`** — pertenece a una `Cofradia`: tipo (`IDA`/`OFICIAL`/`VUELTA`), geometría
  (`LineString` de PostGIS, SRID 4326 — WGS84, mismo sistema que GPS) y distancia en metros.
- **`ResultadoOptimizacion`** — cabecera de un cálculo del solver para un `Escenario`: fecha,
  retraso total y retraso máximo en minutos.
- **`ResultadoCofradia`** — resultado por cofradía dentro de un cálculo: orden asignado, hora de
  entrada/salida calculada del recorrido oficial y retraso en minutos.

## Seguridad y autenticación

Autenticación con **JWT stateless** (sin sesiones de servidor):

1. `POST /api/auth/register` o `POST /api/auth/login` devuelven un token JWT firmado (HMAC-SHA,
   `io.jsonwebtoken`), válido 24 horas por defecto (`jwt.expiration-ms` en `application.yml`).
2. El resto de endpoints bajo `/api/**` exigen la cabecera `Authorization: Bearer <token>`. Un
   [`JwtAuthenticationFilter`](src/main/java/com/cofradias/security/JwtAuthenticationFilter.java)
   valida el token en cada petición y puebla el `SecurityContext`.
3. Sin token válido → **401 Unauthorized**. Con token válido pero intentando acceder a un recurso
   de otro usuario → **403 Forbidden**.

**Autorización por propietario**: cada endpoint que gestiona escenarios, cofradías, recorridos o
resultados comprueba explícitamente que el recurso pertenece al usuario autenticado, recorriendo
la cadena de propiedad (p. ej. `Recorrido → Cofradia → Escenario → Usuario`), no solo que el
usuario esté autenticado. Esta comprobación vive en
[`CurrentUserProvider`](src/main/java/com/cofradias/security/CurrentUserProvider.java) y en cada
controlador.

## API REST

Todos los endpoints devuelven y esperan `application/json`. Los marcados 🔒 requieren cabecera
`Authorization: Bearer <token>`.

### Autenticación (públicos)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/register` | Crea una cuenta. Body: `{nombre, email, password}`. Devuelve `{token, usuario}`. |
| POST | `/api/auth/login` | Inicia sesión. Body: `{email, password}`. Devuelve `{token, usuario}`. |

### Usuarios 🔒

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/usuarios/{id}` | Perfil de un usuario (solo el propio). |

### Escenarios 🔒

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/usuarios/{usuarioId}/escenarios` | Crea un escenario. Body: `{nombre, fecha}`. |
| GET | `/api/usuarios/{usuarioId}/escenarios` | Lista los escenarios de un usuario. |
| GET | `/api/escenarios/{id}` | Detalle de un escenario. |

### Cofradías 🔒

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/escenarios/{escenarioId}/cofradias` | Crea una cofradía. Body: `{nombre, numNazarenos, longitudCortejoMetros, velocidadMarchaMetrosMinuto, horaSalidaEstimada, horaEncierroLimite, horaOficialPaso}`. |
| GET | `/api/escenarios/{escenarioId}/cofradias` | Lista las cofradías de un escenario. |

### Recorridos 🔒

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/cofradias/{cofradiaId}/recorridos` | Crea un tramo. Body: `{tipo: "IDA"\|"OFICIAL"\|"VUELTA", puntos: [{lat, lon}, ...] (mínimo 2), distanciaMetros}`. |
| GET | `/api/cofradias/{cofradiaId}/recorridos` | Lista los tramos de una cofradía. |

### Optimización 🔒

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/escenarios/{escenarioId}/optimizar` | Ejecuta el solver para el escenario y persiste el resultado. Devuelve el resultado completo con el detalle por cofradía. |
| GET | `/api/escenarios/{escenarioId}/resultados` | Lista (resumen) de todos los cálculos previos de un escenario. |
| GET | `/api/resultados/{id}` | Detalle completo de un resultado, con el desglose por cofradía. |

### Códigos de error

| Código | Cuándo |
|---|---|
| 400 | Validación de campos fallida (cuerpo `{detail, errores: {campo: mensaje}}`). |
| 401 | Falta el token, es inválido, o las credenciales de login son incorrectas. |
| 403 | El usuario autenticado no es el propietario del recurso solicitado. |
| 404 | El recurso (escenario/cofradía/recorrido/resultado) no existe. |
| 409 | Email ya registrado. |
| 422 | El escenario no se puede optimizar (faltan datos, o no existe un horario factible). |

## Motor de optimización (Choco Solver)

Implementado en
[`OptimizacionService`](src/main/java/com/cofradias/optimization/OptimizacionService.java).

### Modelo

El recorrido oficial se trata como **un único recurso sin adelantamiento** — igual que una vía de
tren de un solo carril. Cada cofradía *i* "ocupa" ese recurso desde que entra la cabeza del
cortejo hasta que sale su cola:

```
duracion_ocupacion(i) = ceil( (distancia_oficial(i) + longitud_cortejo(i)) / velocidad(i) )
```

**Variable de decisión** por cofradía: `entrada(i)` — el minuto del día (0–1439) en el que la
cofradía *i* entra al recorrido oficial. Su dominio se acota con:

```
entrada_minima(i) = hora_salida_estimada(i) + duracion_transito_IDA(i)
entrada_maxima(i) = hora_encierro_limite(i) − duracion_ocupacion(i) − duracion_transito_VUELTA(i)
```

**Restricción de no-solape**: para cada par de cofradías *(i, j)*, una de las dos tiene que haber
liberado el recurso antes de que la otra entre:

```
entrada(j) ≥ entrada(i) + duracion_ocupacion(i)   O   entrada(i) ≥ entrada(j) + duracion_ocupacion(j)
```

**Función objetivo**: minimizar la suma de los retrasos, donde el retraso de cada cofradía es

```
retraso(i) = max(0, entrada(i) − hora_oficial_paso(i))
```

— es decir, solo penaliza llegar tarde; llegar antes de la hora publicada tiene coste cero.

El solver tiene un límite de 30 segundos de cómputo como salvaguarda (en la práctica, para
decenas de cofradías, resuelve en milisegundos). Si no encuentra ninguna solución factible
(ventanas de tiempo incompatibles entre sí), devuelve **422** con un mensaje explicando el
problema.

### Simplificaciones asumidas

- **No se admite que una cofradía cruce la medianoche** (la hora de encierro límite debe ser,
  en reloj, posterior a la hora de salida estimada dentro del mismo día).
- El cálculo se ejecuta **de forma síncrona** dentro de la petición HTTP — el diseño original
  contemplaba ejecutarlo `@Async` con *polling*/WebSocket, pero para el tamaño de problema actual
  (resuelve en milisegundos) no era necesario; queda como mejora natural si el número de cofradías
  crece mucho.
- El campo `horaSalidaCalculada` de cada cofradía representa cuándo **la cabeza** del cortejo
  termina de recorrer el tramo oficial (no cuándo lo libera por completo, que es
  `duracion_ocupacion`, usado internamente solo para la restricción de no-solape).

## Testing y cobertura

El proyecto tiene **80 tests** (43 unitarios + 37 de integración) con una cobertura, medida con
JaCoCo, de:

| Métrica | Cobertura |
|---|---|
| Líneas | 100 % |
| Métodos | 100 % |
| Clases | 100 % |
| Instrucciones | 99 % |
| Ramas | 94 % |

El pequeño resto de instrucciones/ramas sin cubrir corresponde a bytecode sintético generado por
el compilador (comprobaciones implícitas de multi-catch, etc.) que no se corresponde con ninguna
línea de código fuente real — no hay lógica de negocio sin probar.

### Cómo ejecutar los tests

```bash
# Solo tests unitarios (rapidos, sin base de datos): JwtService, filtro de seguridad,
# UsuarioDetailsService, CurrentUserProvider, OptimizacionService (con Mockito), DTOs,
# GlobalExceptionHandler
./mvnw test

# Suite completa: unitarios + integracion (MockMvc contra la app real) + informe de cobertura.
# Requiere la base de datos levantada primero:
docker compose up -d
./mvnw verify
```

El informe HTML de cobertura se genera en `target/site/jacoco/index.html`.

### Qué prueban los tests de integración

Los tests `*IT` (en `src/test/java/com/cofradias/web/`) ejercitan la aplicación completa —
seguridad, JPA/Hibernate Spatial, controladores — contra el mismo PostgreSQL/PostGIS real que usa
la app en desarrollo (necesario porque el tipo de columna `geometry(LineString,4326)` de
`Recorrido` no existe en una base en memoria como H2). Replican exactamente los escenarios
probados manualmente durante el desarrollo:

- `AuthControllerIT` — registro, login, credenciales inválidas, email duplicado, validación,
  401 sin token / con token manipulado.
- `EscenarioCofradiaRecorridoFlowIT` — flujo completo Usuario → Escenario → Cofradía → Recorrido
  (incluida la geometría real), y la matriz completa de autorización por propietario (403 en cada
  nivel de la cadena, 404 en recursos padre inexistentes).
- `OptimizacionControllerIT` — optimización con margen (retraso 0), optimización con conflicto
  real (cascada de retrasos 0→12→29), errores 422 (datos incompletos, escenario vacío), 404, y
  403 en resultados ajenos.

### Nota sobre Testcontainers

Los tests de integración usan la base de datos de `docker-compose.yml` en lugar de Testcontainers
levantando un contenedor efímero desde la JVM. Se intentó usar Testcontainers primero, pero en la
máquina donde se desarrolló, la versión de Docker Desktop instalada no era compatible con el
transporte de Testcontainers sobre *named pipes* de Windows (el cliente Docker de Testcontainers
recibía respuestas HTTP 400 malformadas del Docker Engine). Es un candidato natural para retomar
en cuanto esa incompatibilidad se resuelva en una versión posterior de Testcontainers.

## Incidencias de entorno encontradas durante el desarrollo

Documentadas aquí porque no son evidentes y pueden volver a aparecer al reproducir el entorno en
otra máquina:

1. **Lombok no genera código en JDK 26 sin lanzar ningún error.** JDK 26 es muy reciente (no LTS)
   y Lombok 1.18.46 no lo soporta todavía; los `@Getter`/`@Builder`/etc. se ignoran en silencio.
   Solución: usar JDK 25 (LTS) o superior con soporte confirmado.
2. **`javac` a partir de JDK 23 ya no descubre annotation processors por classpath de forma
   implícita.** Hubo que declarar explícitamente `annotationProcessorPaths` para Lombok en el
   `maven-compiler-plugin` (ver `pom.xml`).
3. **Spring Boot 4.1 modularizó el soporte de test más de lo habitual en Boot 3.x**:
   `@AutoConfigureMockMvc` vive ahora en el artefacto `spring-boot-webmvc-test`
   (`org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`), no en
   `spring-boot-test-autoconfigure` como en versiones anteriores.
4. **Testcontainers 2.x reestructuró sus módulos**: `testcontainers-bom:2.0.5` ya no gestiona
   artefactos como `junit-jupiter` o `postgresql` por separado. Hubo que fijar
   `testcontainers.version=1.20.1` explícitamente sobre la propiedad heredada del BOM de Spring
   Boot.

## Estado del proyecto y próximos pasos

**Hecho:**
- Modelo de datos completo con persistencia PostGIS real.
- Autenticación JWT con autorización por propietario en toda la cadena de recursos.
- API REST completa para gestionar escenarios, cofradías y recorridos (con geometría).
- Motor de optimización con Choco Solver, probado con casos reales de margen y de conflicto.
- Suite de tests con cobertura muy alta (ver arriba).
- Frontend React + Leaflet completo: registro/login, gestión de escenarios y cofradías, mapa para
  dibujar recorridos con `leaflet-draw`, y visualización de resultados de optimización — ver
  [README del frontend](frontend/README.md).

**Pendiente:**
- Tests automatizados del frontend (el backend ya tiene una suite completa).
- Variable de entorno para la URL de la API en el frontend (ahora mismo está fijada a
  `http://localhost:8080/api`, ver [README del frontend](frontend/README.md#conexión-con-el-backend)).
- Edición y borrado de escenarios/cofradías/recorridos (backend y frontend solo soportan alta y
  consulta por ahora).
- Ejecución asíncrona del solver (`@Async` + *polling* o WebSocket) si el volumen de cofradías por
  escenario crece significativamente.
- Migrar los tests de integración a Testcontainers en cuanto se resuelva la incompatibilidad con
  Docker Desktop en Windows (ver nota arriba).
