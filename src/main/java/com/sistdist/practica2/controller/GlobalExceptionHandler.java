package com.sistdist.practica2.controller;

import org.springframework.dao.DataAccessException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Errores de acceso a datos (PostgreSQL caído, query incorrecta...)
    @ExceptionHandler(DataAccessException.class)
    public String handleDataAccessException(DataAccessException e, Model model) {
        model.addAttribute("tipoError", "Error de Base de Datos");
        model.addAttribute("mensajeError",
                "Se produjo un error al acceder a la base de datos. " +
                        "Comprueba que PostgreSQL está corriendo en Docker.");
        model.addAttribute("detalleError", e.getMostSpecificCause().getMessage());
        model.addAttribute("critico", false);
        return "error";
    }

    // Página no encontrada
    @ExceptionHandler(NoHandlerFoundException.class)
    public String handleNotFound(NoHandlerFoundException e, Model model) {
        model.addAttribute("tipoError", "Página no encontrada (404)");
        model.addAttribute("mensajeError", "La URL solicitada no existe: " + e.getRequestURL());
        model.addAttribute("detalleError", "");
        model.addAttribute("critico", false);
        return "error";
    }

    // Cualquier otra excepción no controlada
    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception e, Model model) {
        model.addAttribute("tipoError", "Error inesperado");
        model.addAttribute("mensajeError",
                "Ha ocurrido un error inesperado en la aplicación. " +
                        "Si el problema persiste, contacta con el administrador.");
        model.addAttribute("detalleError", e.getClass().getSimpleName() + ": " + e.getMessage());
        model.addAttribute("critico", true);
        return "error";
    }
}