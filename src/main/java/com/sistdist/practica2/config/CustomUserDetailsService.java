package com.sistdist.practica2.config;

import com.sistdist.practica2.model.User;
import com.sistdist.practica2.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    // @Service = componente de Spring. Implements UserDetailsService = contrato
    // que Spring Security exige para saber como cargar usuarios

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {

        this.userRepository = userRepository;
        // Spring inyecta el repository automaticamente por constructor
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        // Spring Security llama a este metodo cuando alguien hace login
        User user = userRepository.findUserByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("Usuario no encontrado: " + username);
            // si no existe el usuario Spring Security deniega el acceso
        }
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority(user.getUserRole().getRoleName()))
        );
        // devuelve un objeto que Spring Security entiende con:
        // username, contraseña cifrada y rol (ROLE_ADMIN o ROLE_USER)
    }
}