## 🐳 Docker y configuración del entorno

### `docker-compose.yml`
Archivo de definición y orquestación de contenedores que gestiona el despliegue 
de los servicios. En este proyecto, define la ejecución de dos contenedores 
principales: uno para **PostgreSQL 16** y otro para la **API Flask**. Incluye 
la configuración de dependencias entre contenedores mediante directivas de 
*healthcheck*, garantizando que la API Flask no inicie su ejecución hasta que 
el servicio de base de datos esté completamente levantado y listo para aceptar 
conexiones, evitando así errores de inicialización.

### `python-api/Dockerfile`
Archivo de configuración que define la construcción de la imagen de la API en 
Flask. Establece la imagen base (Python 3.12), gestiona la instalación de las 
dependencias definidas en el sistema y configura el punto de entrada para 
arrancar la aplicación mediante `app.py`.

### `python-api/requirements.txt`
Documento que lista las dependencias necesarias para el entorno de Python. 
Incluye `flask` (framework web), `psycopg2-binary` (driver para la conexión 
con PostgreSQL) y `requests` (librería para la comunicación HTTP con la PokeAPI).

---

## ⚙️ Configuración de Spring Boot

### `pom.xml`
Archivo de configuración de Maven que gestiona las dependencias del proyecto 
Java. Integra librerías fundamentales como **Spring Security**, **Thymeleaf**, 
**Spring Data JPA** y **Lombok**. Además, incluye el driver específico para 
conectarse a una base de datos PostgreSQL, la cual es el motor de base de datos
relacional designado para la persistencia del sistema.

### `application.properties`
Archivo de configuración principal de Spring Boot. Centraliza las variables de
entorno del aplicativo, incluyendo la cadena de conexión a la base de datos 
(`localhost:5432`), las credenciales de acceso, la configuración del dialecto
SQL (`PostgreSQLDialect`) y la definición de la URL base para la comunicación
con la API externa en Flask (`localhost:5000`).

---

## ☕ Clases Java — Modelo de datos

### `model/Role.java`
Entidad que representa un rol de usuario dentro del sistema. Contiene los 
atributos `id`, `roleName` (ej. `ROLE_ADMIN`, `ROLE_USER`) y `showOnCreate`.
Se encuentra mapeada mediante JPA a la tabla `role` en PostgreSQL y actúa 
como el lado "uno" en la relación de base de datos de uno-a-muchos con la 
entidad de usuarios.

### `model/User.java`
Entidad que representa a un usuario del sistema. Define los campos `id`, 
`username`, `email` y `password` (la cual se almacena cifrada mediante el 
algoritmo BCrypt). Incluye una relación `@ManyToOne` hacia la entidad `Role`.
Cabe destacar que esta entidad se mapea a la tabla `users` en la base de datos
(se utiliza este nombre en plural dado que `user` es una palabra reservada en 
la sintaxis de PostgreSQL).

### `repository/UserRepository.java` y `repository/RoleRepository.java`
Interfaces basadas en el patrón Repository que extienden de `JpaRepository`. 
Delegan en Spring Data JPA la generación automática de consultas SQL mediante 
el nombrado de métodos (como `findUserByUsername` o `findByRoleName`), 
separando así la capa de acceso a datos de la lógica de negocio.

---

## ☕ Clases Java — Seguridad

### `config/CustomUserDetailsService.java`
Clase que implementa la lógica de autenticación requerida por Spring 
Security. Se encarga de buscar al usuario en la base de datos mediante el 
`UserRepository`. Si la validación es correcta, retorna un objeto `UserDetails`
con las credenciales y el rol asignado; en caso contrario, lanza una excepción 
`UsernameNotFoundException`.

### `config/SecurityConfig.java`
Clase de configuración central de la seguridad del sistema. Define las 
políticas de control de acceso: establece qué rutas son de acceso público 
(raíz, login, recursos estáticos) y cuáles requieren autenticación o roles 
específicos. También gestiona el enrutamiento post-login (hacia `/dashboard`)
y el manejo de errores de autenticación.

### `config/RestTemplateConfig.java`
Clase de configuración que provee un *bean* de `RestTemplate`. Este componente
es utilizado por Spring Boot como cliente HTTP para efectuar las llamadas REST
hacia la API de Flask, permitiendo su inyección de dependencias (`@Autowired`)
en los controladores.

---

## ☕ Clases Java — Controladores

### `controller/MainController.java`
Controlador encargado de la navegación base del aplicativo. Gestiona el 
enrutamiento hacia la vista pública (`/`), el panel de control (`/dashboard`)
y el sistema de acceso (`/login`), manejando también los parámetros de 
respuesta visuales para errores de autenticación y cierres de sesión.

### `controller/ApiSimulatorController.java`
Controlador principal de la lógica de negocio de la práctica. Actúa como 
intermediario recibiendo las peticiones del usuario, construyendo las 
peticiones hacia la API en Flask a través de `RestTemplate` y capturando 
las respuestas. Implementa un manejo detallado de excepciones HTTP:
*   `HttpClientErrorException`: Para errores de cliente (4xx) generados por Flask.
*   `HttpServerErrorException`: Para errores de servidor (5xx).
*   `ResourceAccessException`: Para fallos de disponibilidad del servicio Flask.
*   `RestClientException`: Para excepciones REST genéricas.
    Se encarga de traducir estos códigos de error técnicos en mensajes comprensibles para el usuario final.

### `controller/GlobalExceptionHandler.java`
Clase anotada con `@ControllerAdvice` que centraliza el manejo de excepciones 
no controladas en toda la aplicación. Intercepta excepciones técnicas como 
`DataAccessException` (errores de base de datos) o `NoHandlerFoundException`
y redirige el flujo hacia una página de error amigable, evitando exponer 
detalles técnicos al cliente.

---

## ☕ Clases Java — DTO (Data Transfer Object)

### `dto/ApiResponseDto.java`
Objeto de transferencia de datos encargado de la deserialización de las 
respuestas JSON provenientes de Flask. Mapea los atributos de la respuesta 
(`resultado`, `error`, `detalle`, `tipo`, `nombre`, etc.). Incluye la anotación
`@JsonIgnoreProperties(ignoreUnknown = true)` para asegurar la robustez del 
paseo en caso de que la API devuelva campos no contemplados en el modelo.

---

## 🐍 API Flask

### `python-api/app.py`
Módulo principal que contiene la lógica de la API REST desarrollada en Python.
Expone diez *endpoints* estructurados para simular distintos escenarios de error. 
Implementa bloques `try/except` que capturan las excepciones de Python y 
retornan un JSON estructurado que incluye un campo `tipo` con la clase de la 
excepción generada, facilitando así su posterior tratamiento por parte de 
Spring Boot.

---

## 🌐 Vistas Thymeleaf

*   **`templates/index.html`**: Vista pública inicial. Implementa directivas `sec:authorize` de Spring Security para renderizar condicionalmente el contenido dependiendo del estado de autenticación del usuario.
*   **`templates/login.html`**: Formulario de autenticación integrado con las directivas de seguridad y manejo dinámico de mensajes de error.
*   **`templates/dashboard.html`**: Panel de control privado. Muestra información contextual y utiliza `sec:authentication="name"` para extraer datos de la sesión activa.
*   **`templates/api-simulator.html`**: Interfaz principal de la práctica. Contiene los formularios que disparan las peticiones hacia el controlador de Spring. Muestra los resultados mediante paneles dinámicos codificados por colores según el éxito o fracaso de la operación, mostrando el tipo de excepción capturada.
*   **`templates/error.html`**: Vista de error global. Sustituye la visualización técnica del *stacktrace* por una interfaz amigable que expone únicamente la información relevante del fallo detectado.
*   **`static/css/styles.css`**: Hoja de estilos en cascada que define el diseño visual de la aplicación, implementando una paleta de colores y tipografías específicas para el contexto del proyecto.

---

## 🗂️ Clase Principal de Ejecución

### `Practica2Application.java`
Clase de arranque del framework Spring Boot. Además del método `main`, 
implementa la interfaz `CommandLineRunner`. Esta implementación permite 
ejecutar lógica de inicialización en el despliegue del servidor: verifica 
el estado de la base de datos e inserta usuarios y roles predeterminados 
(aplicando el cifrado BCrypt) si las tablas se encuentran vacías, asegurando
que el sistema sea funcional y autocontenido desde su primer inicio.