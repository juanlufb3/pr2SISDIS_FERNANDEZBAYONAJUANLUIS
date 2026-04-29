package com.sistdist.practica2.dto;
//Java necesita un objeto tipado para trabajar con los datos.
// RestTemplate deserializa el JSON de Flask a este objeto automáticamente.
// Sin el DTO tendría que analizar el JSON a mano.

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data //genera getters, setters, toString y equals en una sola anotacion
@JsonIgnoreProperties(ignoreUnknown = true)
//si Flask devuelve campos que no estan aqui los ignora sin que epte
public class ApiResponseDto {
    private String resultado;
    // campo de respuestas exitosas: "Conexion correcta a PostgreSQL 16..."

    private String error;
    // campo de respuestas de error: "Archivo no encontrado"

    private String detalle;
    // detalle tecnico de la excepcion Python

    private String tipo;
    // nombre exacto de la excepcion: "FileNotFoundError", "OperationalError"...

    // campos especificos de pokemon:
    private String nombre;  // "pikachu"
    private Integer id;     // 25
    private Integer peso;   // 60
    private Integer altura; // 4
}