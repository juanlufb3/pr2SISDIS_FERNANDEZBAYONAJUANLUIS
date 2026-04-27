package com.sistdist.practica2.controller;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controlador de DEMOSTRACIÓN para la memoria de la práctica.
 * Cada endpoint fuerza una excepción distinta que captura GlobalExceptionHandler.
 *
 * URLs de prueba (requieren login):
 *   /demo/error-bd         → DataAccessException     → error.html
 *   /demo/error-runtime    → RuntimeException        → error.html (crítico)
 *   /demo/error-null       → NullPointerException    → error.html (crítico)
 */
@Controller
@RequestMapping("/demo")
public class DemoExceptionController {

    /** Simula un fallo de acceso a base de datos desde Spring */
    @GetMapping("/error-bd")
    public String errorBd() {
        throw new DataAccessResourceFailureException(
                "Simulación: PostgreSQL no responde en localhost:5432"
        );
    }

    /** Simula un error de negocio genérico */
    @GetMapping("/error-runtime")
    public String errorRuntime() {
        throw new RuntimeException(
                "Simulación: condición de negocio no esperada en el servidor"
        );
    }

    /** Simula un NullPointerException (error de programación) */
    @GetMapping("/error-null")
    public String errorNull() {
        String s = null;
        s.length(); // NullPointerException intencionado
        return "index";
    }
}