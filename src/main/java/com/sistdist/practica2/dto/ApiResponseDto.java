package com.sistdist.practica2.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiResponseDto {
    private String resultado;
    private String error;
    private String detalle;
    private String tipo;
    // Campos específicos Pokémon
    private String nombre;
    private Integer id;
    private Integer peso;
    private Integer altura;
}