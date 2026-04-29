package com.sistdist.practica2.model;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
//@Entity le dice al Hibernate "esta clase es una tabla en la base ded datos"
@Getter @Setter
//genera todos los getters y setters (Lombok)
@AllArgsConstructor @NoArgsConstructor
//genera un constructor vacio (Lombok)
@Table(name = "role")
//el nombre exacto de la tabla
public class Role implements Serializable {

    @Id //indicamos que esta es la clave primaria
    @GeneratedValue(strategy = GenerationType.IDENTITY) //el id se genera solo
    private Integer id;

    @Column(nullable = false, unique = true) //traduce a NOT NULL y UNIQUE en SQL
    private String roleName;

    @Column(nullable = false)
    private Integer showOnCreate;
}