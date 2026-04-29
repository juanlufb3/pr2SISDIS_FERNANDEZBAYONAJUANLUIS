package com.sistdist.practica2.model;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@Table(name = "users")  // "user" es palabra reservada en PostgreSQL
public class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // el id se genera solo
    private Integer id;

    @Column(name = "username", length = 50, unique = true, nullable = false)
    private String username;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "password", length = 250, nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.EAGER) //muchos usuarios pueden tener el mismo rol
    @JoinColumn(name = "role_id") // cuando cargues un usuario, carga su rol tambien en la misma consulta
    private Role userRole;
}