package com.sistdist.practica2.controller;

import com.sistdist.practica2.dto.ApiResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.*;import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/api-simulator")
// todas las rutas de este controlador empiezan por /api-simulator
public class ApiSimulatorController {

    private static final Logger log = LoggerFactory.getLogger(ApiSimulatorController.class);

    @Value("${python.api.base-url}")
    private String pythonApiBaseUrl;
    // @Value inyecta el valor de application.properties
    // pythonApiBaseUrl = "http://localhost:5000"

    private final RestTemplate restTemplate;

    public ApiSimulatorController(RestTemplate restTemplate) {

        this.restTemplate = restTemplate;
        // RestTemplate es el cliente HTTP para llamar a Flask
    }

    @GetMapping
    public String vistaSimulador() {

        return "api-simulator";
        // GET /api-simulator simplemente muestra la pantalla
    }

    @PostMapping("/llamar")
    public String llamarApi(@RequestParam String endpoint, Model model) {
        // recibe el endpoint de Flask desde el formulario
        // ej: endpoint = "/api/file-error"
        String url = pythonApiBaseUrl + endpoint;
        // construye la URL completa: http://localhost:5000/api/file-error
        model.addAttribute("endpointLlamado", url);
        log.info(">>> Invocando API Python: {}", url);

        try {
            ResponseEntity<ApiResponseDto> respuesta =
                    restTemplate.getForEntity(url, ApiResponseDto.class);
            // hace el GET a Flask y deserializa el JSON en ApiResponseDto
            log.info("<<< Respuesta OK: HTTP {}", respuesta.getStatusCode().value());
            model.addAttribute("exito", true);
            model.addAttribute("resultado", respuesta.getBody());
            model.addAttribute("statusCode", respuesta.getStatusCode().value());
            // mete los datos en el modelo para que Thymeleaf los muestre
            // en el panel verde del simulador

        } catch (HttpClientErrorException e) {
            // Flask devolvio 4xx (404, 403...)
            // ej: pokemon inexistente, archivo no encontrado
            log.warn("<<< Error 4xx en {}: {} - {}", url, e.getStatusCode().value(), e.getMessage());
            model.addAttribute("exito", false);
            model.addAttribute("tipoExcepcion", "HttpClientErrorException " + e.getStatusCode().value());
            model.addAttribute("mensajeError", traducirError4xx(e));
            model.addAttribute("detalleError", e.getResponseBodyAsString());

        } catch (HttpServerErrorException e) {
            // Flask devolvio 5xx (500, 504...)
            // ej: fallo de BD, timeout de PokeAPI
            log.error("<<< Error 5xx en {}: {} - {}", url, e.getStatusCode().value(), e.getMessage());
            model.addAttribute("exito", false);
            model.addAttribute("tipoExcepcion", "HttpServerErrorException " + e.getStatusCode().value());
            model.addAttribute("mensajeError", traducirError5xx(e));
            model.addAttribute("detalleError", e.getResponseBodyAsString());

        } catch (ResourceAccessException e) {
            // Flask no esta disponible, Docker parado o contenedor caido
            log.error("<<< API Python NO disponible en {}: {}", url, e.getMessage());
            model.addAttribute("exito", false);
            model.addAttribute("tipoExcepcion", "ResourceAccessException (conexión rechazada)");
            model.addAttribute("mensajeError",
                    "No se pudo conectar con la API Python. ¿Está Docker corriendo? Ejecuta: docker compose up -d");
            model.addAttribute("detalleError", e.getMessage());

        } catch (RestClientException e) {
            // cualquier otro error REST no previsto
            log.error("<<< Error REST inesperado: {}", e.getMessage());
            model.addAttribute("exito", false);
            model.addAttribute("tipoExcepcion", "RestClientException");
            model.addAttribute("mensajeError", "Error inesperado al llamar a la API: " + e.getMessage());
            model.addAttribute("detalleError", "");
        }

        return "api-simulator";
        // siempre devuelve la misma vista, con exito=true o exito=false
        // la app NUNCA peta, siempre muestra algo al usuario
    }
    private String traducirError4xx(HttpClientErrorException e) {
        return switch (e.getStatusCode().value()) {
            case 400 -> "Petición incorrecta: los parámetros enviados no son válidos.";
            case 404 -> "Recurso no encontrado (404). Puede que el Pokémon no exista o el archivo no esté disponible.";
            case 401 -> "No autorizado para acceder a este recurso (401).";
            case 403 -> "Acceso prohibido al recurso (403).";
            case 408 -> "Tiempo de espera agotado al llamar a la API externa (408).";
            default  -> "Error del cliente en la llamada a la API (" + e.getStatusCode().value() + ").";
        };
        // switch por codigo HTTP → mensaje en español legible para el usuario
    }

    private String traducirError5xx(HttpServerErrorException e) {
        return switch (e.getStatusCode().value()) {
            case 500 -> "Error interno en el servidor Python (500). Puede ser un fallo de base de datos o de lectura de archivo.";
            case 502 -> "Pasarela incorrecta (502). La API no pudo conectar con un servicio externo.";
            case 503 -> "Servicio no disponible temporalmente (503). Inténtalo de nuevo más tarde.";
            case 504 -> "Timeout al llamar a la PokeAPI u otro servicio externo (504).";
            default  -> "Error del servidor Python (" + e.getStatusCode().value() + ").";
        };
    }
}