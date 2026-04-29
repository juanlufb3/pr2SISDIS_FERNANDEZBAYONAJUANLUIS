package com.sistdist.practica2.repository;

import com.sistdist.practica2.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository //marca la interfaz como componente de acceso a datos para que Spring la detecte y la inyecte donde haga falta.
public interface UserRepository extends JpaRepository<User, Integer> {
    User findUserByUsername(String username);
    User findUserByEmail(String email);
}