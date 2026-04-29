# Práctica 2 — Sistemas Distribuidos
## Sistema de Gestión de Excepciones con Spring Boot y Flask

Práctica obligatoria 2 de la asignatura Sistemas Distribuidos, 4º Ingeniería Informática.

El objetivo es implementar un sistema distribuido de tres capas que demuestre la gestión robusta de excepciones entre un frontend Spring Boot, una API REST en Python con Flask y una base de datos PostgreSQL, todo orquestado con Docker Compose.

---

## Índice

- [Arquitectura](#arquitectura)
- [Tecnologías](#tecnologías)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Requisitos previos](#requisitos-previos)
- [Configuración y arranque](#configuración-y-arranque)
- [Endpoints de la API Flask](#endpoints-de-la-api-flask)
- [Gestión de excepciones implementada](#gestión-de-excepciones-implementada)
- [Seguridad](#seguridad)
- [Pruebas con Postman](#pruebas-con-postman)
- [Comandos Docker](#comandos-docker)
- [Notas técnicas](#notas-técnicas)

---

## Arquitectura

El sistema sigue una arquitectura de tres capas distribuidas con comunicación HTTP entre ellas:

```
Usuario (Navegador)
        │
        │ HTTP :8080
        ▼
┌─────────────────────────────────────┐
│         Spring Boot (Local)          │
│                                     │
│  Spring MVC + Thymeleaf             │
│  Spring Security (BCrypt)           │
│  RestTemplate → llamadas a Flask    │
│  GlobalExceptionHandler             │
│  JPA / Hibernate                    │
└────────────┬──────────────┬─────────┘
             │              │
     HTTP :5000        JDBC :5432
             │              │
    ┌────────▼──────┐  ┌────▼──────────────┐
    │  Flask (Docker)│  │ PostgreSQL (Docker)│
    │               │  │                   │
    │  /api/file-*  │  │  Tabla: users     │
    │  /api/db-*    │  │  Tabla: role      │
    │  /api/pokemon │  │                   │
    │  /api/stats   │  └───────────────────┘
    └───────┬───────┘
            │ HTTPS
    ┌───────▼───────┐
    │ PokeAPI        │
    │ pokeapi.co     │
    └───────────────┘
```

Spring Boot actúa como controlador: recibe las peticiones del usuario, delega en la API Flask mediante `RestTemplate`, captura las excepciones que devuelve Flask según el código HTTP de respuesta y las presenta al usuario traducidas al español sin exponer stacktraces.

Flask es el núcleo de las excepciones: implementa los tres tipos requeridos por el enunciado (archivos, base de datos y API externa), captura cada excepción con bloques `try/except` específicos y devuelve siempre un JSON estructurado con los campos `error`, `tipo` y `detalle`.

PostgreSQL persiste los usuarios y roles del sistema de autenticación. Corre en Docker y Spring Boot se conecta a él mediante JPA/Hibernate.

---

## Tecnologías

| Tecnología | Versión | Rol en el proyecto |
|---|---|---|
| Java | 21 LTS | Lenguaje del frontend |
| Spring Boot | 3.4.0 | Framework principal |
| Spring Security | 6.x | Autenticación y autorización |
| Spring Data JPA | 3.4.0 | Acceso a datos con Hibernate |
| Thymeleaf | 3.x | Motor de plantillas HTML |
| PostgreSQL Driver | 42.x | Conector JDBC (sustituye a MySQL) |
| Lombok | 1.18.x | Reducción de boilerplate |
| Python | 3.12 | Lenguaje de la API Flask |
| Flask | 3.1.0 | Framework de la API REST |
| psycopg2-binary | 2.9.10 | Driver Python para PostgreSQL |
| requests | 2.32.3 | Cliente HTTP para PokeAPI |
| PostgreSQL | 16 Alpine | Base de datos relacional |
| Docker | 27.x | Contenerización |
| Docker Compose | v2 | Orquestación multi-contenedor |
| Maven | 3.9.x | Gestión de dependencias Java |

---

## Estructura del proyecto

```
practica2/
├── docker-compose.yml
├── pom.xml
│
├── documentacion
    ├── capturas_postman/
    │   ├── 01_healthcheck.png
    │   ├── ...
    ├── informe-pr2-juanluisfernandez/
    │   ├── Informe_Practica2_SistDistribuidos.docx
    │   ├── Informe_Practica2_SistDistribuidos.pdf
    └── Practica 2 - Sist Dist.postman_collection.json
│
├── python-api/
│   ├── app.py
│   ├── requirements.txt
│   └── Dockerfile
│
└── src/
    ├── main/
    │   ├── java/com/sistdist/practica2/
    │   │   ├── Practica2Application.java
    │   │   ├── config/
    │   │   │   ├── CustomUserDetailsService.java
    │   │   │   ├── SecurityConfig.java
    │   │   │   └── RestTemplateConfig.java
    │   │   ├── controller/
    │   │   │   ├── MainController.java
    │   │   │   ├── ApiSimulatorController.java
    │   │   │   └── GlobalExceptionHandler.java
    │   │   ├── dto/
    │   │   │   └── ApiResponseDto.java
    │   │   ├── model/
    │   │   │   ├── User.java
    │   │   │   └── Role.java
    │   │   └── repository/
    │   │       ├── UserRepository.java
    │   │       └── RoleRepository.java
    │   └── resources/
    │       ├── application.properties
    │       ├── templates/
    │       │   ├── index.html
    │       │   ├── login.html
    │       │   ├── dashboard.html
    │       │   ├── api-simulator.html
    │       │   └── error.html
    │       └── static/css/
    │           └── styles.css
    └── test/
        └── java/com/sistdist/practica2/
            └── Practica2ApplicationTests.java
```

### Descripción de los componentes principales

**`Practica2Application.java`** — Punto de entrada. Implementa `CommandLineRunner` para insertar datos iniciales (roles y usuarios) en la base de datos al arrancar, únicamente si las tablas están vacías.

**`config/SecurityConfig.java`** — Configura Spring Security: reglas de acceso por URL, página de login personalizada, redirecciones tras login/logout y el `PasswordEncoder` BCrypt.

**`config/CustomUserDetailsService.java`** — Implementa `UserDetailsService`. Spring Security lo invoca durante el login para cargar el usuario desde PostgreSQL y construir el objeto `UserDetails`.

**`controller/ApiSimulatorController.java`** — Controlador central de la práctica. Realiza las llamadas HTTP a Flask con `RestTemplate` y captura cuatro tipos distintos de excepción REST, traduciendo cada una a un mensaje comprensible.

**`controller/GlobalExceptionHandler.java`** — Anotado con `@ControllerAdvice`. Intercepta excepciones no controladas en cualquier controlador y las redirige a la vista de error amigable.

**`dto/ApiResponseDto.java`** — Objeto de transferencia de datos para deserializar las respuestas JSON de Flask. La anotación `@JsonIgnoreProperties(ignoreUnknown = true)` evita errores si Flask devuelve campos no contemplados.

**`python-api/app.py`** — API Flask con diez endpoints que cubren los tres tipos de excepciones del enunciado. Cada excepción se captura con `try/except` específico y se devuelve como JSON estructurado con el tipo exacto de la excepción Python.

---

## Requisitos previos

- **Docker Desktop** instalado y con el motor arrancado
- **IntelliJ IDEA** con el proyecto abierto
- **Java 21 JDK**
- **Conexión a internet** para descarga de imágenes Docker y acceso a PokeAPI
- **Postman** para las pruebas de API (opcional)

---

## Configuración y arranque

### 1. Verificar Docker Desktop

Comprobar que el motor de Docker está corriendo. El icono de la bandeja del sistema debe estar en verde.

### 2. Levantar los contenedores

Abrir una terminal en la raíz del proyecto (donde está `docker-compose.yml`) y ejecutar:

```bash
docker compose up -d --build
```

El flag `--build` construye la imagen de Flask desde el `Dockerfile`. La primera ejecución descarga las imágenes base y puede tardar varios minutos. Las siguientes ejecuciones son inmediatas.

### 3. Verificar el estado de los contenedores

```bash
docker compose ps
```

Resultado esperado:

```
NAME               STATUS
sisdis_postgres    running (healthy)
sisdis_flask       running
```

> El estado `healthy` en PostgreSQL indica que el healthcheck `pg_isready` se ha ejecutado correctamente. Flask no arranca hasta que PostgreSQL alcanza este estado, gracias a la directiva `depends_on: condition: service_healthy` del `docker-compose.yml`.

### 4. Verificar la API Flask

```
GET http://localhost:5000/health
```

Respuesta esperada:
```json
{"servicio": "Flask API - SisDis Practica 2", "status": "ok"}
```

### 5. Arrancar Spring Boot

Ejecutar `Practica2Application.java` desde IntelliJ (botón Run o `Shift+F10`).

Al arrancar, el `CommandLineRunner` comprueba si la base de datos está vacía e inserta los datos iniciales:

```
✅ Usuarios creados: admin/admin123 | usuario/user123
Started Practica2Application in X.XXX seconds
```

### 6. Acceder a la aplicación

```
http://localhost:8080
```

---

## Endpoints de la API Flask

Base URL: `http://localhost:5000`

| Método | Endpoint | Descripción | HTTP |
|---|---|---|---|
| GET | `/health` | Health check del servicio | 200 |
| GET | `/api/file-ok` | Lectura de archivo existente | 200 |
| GET | `/api/file-error` | FileNotFoundError simulado | 404 |
| GET | `/api/db-ok` | Consulta versión de PostgreSQL | 200 |
| GET | `/api/db-error` | OperationalError con credenciales inválidas | 500 |
| GET | `/api/pokemon/<nombre>` | Consulta Pokémon en PokeAPI | 200 |
| GET | `/api/pokemon-error` | HTTPError 404 de PokeAPI | 404 |
| GET | `/api/pokemon-timeout` | Timeout de API externa forzado | 504 |
| GET | `/api/stats` | Estadísticas de llamadas en sesión | 200 |

---

## Gestión de excepciones implementada

### Excepciones de archivos (Python)

| Excepción | Endpoint | Código HTTP |
|---|---|---|
| `FileNotFoundError` | `/api/file-error` | 404 |
| `PermissionError` | `/api/file-error` | 403 |
| `OSError` | `/api/file-error` | 500 |

### Excepciones de base de datos (psycopg2)

| Excepción | Endpoint | Código HTTP |
|---|---|---|
| `psycopg2.OperationalError` | `/api/db-error` | 500 |
| `psycopg2.DatabaseError` | `/api/db-error` | 500 |

### Excepciones de API externa (requests + PokeAPI)

| Excepción | Endpoint | Código HTTP |
|---|---|---|
| `requests.HTTPError` | `/api/pokemon-error` | 404 |
| `requests.Timeout` | `/api/pokemon-timeout` | 504 |
| `requests.ConnectionError` | `/api/pokemon/<nombre>` | 502 |

### Excepciones capturadas en Spring Boot

El `ApiSimulatorController` captura las excepciones de comunicación con Flask en cuatro bloques `catch` diferenciados:

| Excepción Spring | Cuándo se lanza | Tratamiento |
|---|---|---|
| `HttpClientErrorException` | Flask responde 4xx | Traducción por código HTTP |
| `HttpServerErrorException` | Flask responde 5xx | Traducción por código HTTP |
| `ResourceAccessException` | Flask no disponible / Docker parado | Mensaje de conexión rechazada |
| `RestClientException` | Cualquier otro error REST | Mensaje genérico |

El `GlobalExceptionHandler` (`@ControllerAdvice`) actúa como segundo nivel de seguridad:

| Excepción | Causa |
|---|---|
| `DataAccessException` | Errores de acceso a PostgreSQL |
| `NoHandlerFoundException` | URL inexistente |
| `Exception` | Cualquier excepción no controlada |

En ningún caso se expone un stacktrace al usuario. Todos los errores se traducen a mensajes en español y se presentan en la vista mediante Thymeleaf.

---

## Seguridad

La autenticación se implementa con Spring Security 6.x:

- Las contraseñas se almacenan cifradas con **BCryptPasswordEncoder**
- `CustomUserDetailsService` carga los usuarios desde PostgreSQL
- Las rutas públicas son: `/`, `/css/**`, `/login`, `/error`
- El resto de rutas requieren autenticación
- `/admin/**` requiere rol `ROLE_ADMIN`

### Usuarios de prueba

Insertados automáticamente al primer arranque:

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `admin123` | `ROLE_ADMIN` |
| `usuario` | `user123` | `ROLE_USER` |

### Esquema de base de datos

Hibernate genera las tablas automáticamente con `ddl-auto=update`:

```sql
CREATE TABLE role (
    id             SERIAL PRIMARY KEY,
    role_name      VARCHAR(255) NOT NULL UNIQUE,
    show_on_create INTEGER NOT NULL
);

CREATE TABLE users (
    id       SERIAL PRIMARY KEY,
    username VARCHAR(50)  NOT NULL UNIQUE,
    email    VARCHAR(100),
    password VARCHAR(250) NOT NULL,
    role_id  INTEGER REFERENCES role(id)
);
```

> La tabla se denomina `users` y no `user` porque `user` es una palabra reservada en PostgreSQL. En MySQL no existe esta restricción.

---

## Pruebas con Postman

### Colección: Flask API Directa

Sin autenticación. Método GET sobre `http://localhost:5000`:

```
GET /health                      → 200 OK
GET /api/file-ok                 → 200 OK
GET /api/file-error              → 404 Not Found    (FileNotFoundError)
GET /api/db-ok                   → 200 OK
GET /api/db-error                → 500 Int. Error   (OperationalError)
GET /api/pokemon/pikachu         → 200 OK
GET /api/pokemon-error           → 404 Not Found    (HTTPError)
GET /api/pokemon-timeout         → 504 Gateway T.   (Timeout)
GET /api/stats                   → 200 OK
```

### Colección: Spring Boot → Flask

Autenticación: `Auth → Basic Auth → admin / admin123`

Método: `POST http://localhost:8080/api-simulator/llamar`

Body: `x-www-form-urlencoded`

| Key | Value | Resultado esperado |
|---|---|---|
| endpoint | /api/file-error | HTML con panel rojo, mensaje en español |
| endpoint | /api/db-error | HTML con panel rojo, error 500 traducido |
| endpoint | /api/pokemon-error | HTML con panel rojo, error 404 traducido |

### Test: ResourceAccessException (Flask caído)

```bash
# Detener Flask
docker compose stop python-api

# Enviar POST a Spring Boot con cualquier endpoint
# Resultado: panel rojo con "No se pudo conectar con la API Python"

# Restaurar Flask
docker compose up -d python-api
```

---

## Comandos Docker

```bash
# Estado de los contenedores
docker compose ps

# Logs de Flask en tiempo real
docker compose logs -f python-api

# Logs de PostgreSQL
docker compose logs postgres

# Detener todos los contenedores (conserva datos)
docker compose down

# Detener y eliminar volúmenes (reset completo de la BD)
docker compose down -v

# Reconstruir Flask tras modificar app.py
docker compose up -d --build python-api

# Reiniciar Flask
docker compose restart python-api

# Detener solo Flask (simular API caída)
docker compose stop python-api
```

---

## Notas técnicas

- **PostgreSQL vs MySQL**: el proyecto base de clase usaba MySQL. Esta práctica requiere PostgreSQL. El cambio afecta al driver en `pom.xml` (`org.postgresql:postgresql` en lugar de `mysql-connector-j`), al dialecto de Hibernate (`PostgreSQLDialect`) y al nombre de la tabla de usuarios (`users` en lugar de `user`).

- **Comunicación interna Docker**: Flask se conecta a PostgreSQL usando el hostname `postgres`, que es el nombre del servicio definido en `docker-compose.yml`. Dentro de la red Docker los contenedores se resuelven por nombre de servicio, no por `localhost`.

- **Spring Boot fuera de Docker**: el frontend corre en IntelliJ directamente, no en Docker. Esto facilita la depuración y el ciclo de desarrollo. Se conecta a PostgreSQL y Flask mediante los puertos expuestos en `localhost`.

- **Healthcheck y orden de arranque**: `docker-compose.yml` configura un healthcheck sobre PostgreSQL (`pg_isready`) con `depends_on: condition: service_healthy` en Flask. Esto garantiza que Flask no intenta conectarse a la base de datos hasta que PostgreSQL está completamente operativo.

- **Datos iniciales**: el `CommandLineRunner` en `Practica2Application.java` comprueba `roleRepo.count() == 0` antes de insertar. Si se reinicia Spring Boot sin borrar la base de datos, no intenta insertar duplicados y no lanza excepciones de clave única.

- **Reconstrucción de Flask**: si se modifica `app.py`, es necesario reconstruir la imagen Docker con `docker compose up -d --build python-api`. Sin el flag `--build`, Docker usa la imagen cacheada y los cambios no se aplican.

- **Estadísticas de sesión**: el endpoint `/api/stats` usa un diccionario Python en memoria. Los contadores se resetean al reiniciar el contenedor Flask (`docker compose restart python-api` o `docker compose down`).