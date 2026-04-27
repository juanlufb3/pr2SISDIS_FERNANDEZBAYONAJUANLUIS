package com.sistdist.practica2.model;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@Table(name = "role")
public class Role implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String roleName;

    @Column(nullable = false)
    private Integer showOnCreate;
}