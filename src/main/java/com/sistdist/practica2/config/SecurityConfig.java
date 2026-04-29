package com.sistdist.practica2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration // clase de configuracion, Spring la lee al arrancar
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
        // BCrypt es el algoritmo de cifrado de contraseñas.
        // Convierte "admin123" en algo como "$2a$10$xyz..."
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // desactiva la proteccion CSRF, necesario para que funcionen
                // los formularios POST en Thymeleaf sin token extra
                .userDetailsService(customUserDetailsService)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/css/**", "/login", "/error").permitAll()
                        // estas URLs son publicas, cualquiera puede acceder
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // solo usuarios con ROLE_ADMIN pueden entrar aqui
                        .anyRequest().authenticated()
                        // el resto requiere estar logado
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        // usa nuestra pagina de login en vez de la de Spring por defecto
                        .defaultSuccessUrl("/dashboard", true)
                        // tras login correcto redirige a /dashboard
                        .failureUrl("/login?error=true")
                        // tras login incorrecto redirige aqui, MainController
                        // detecta el parametro error y muestra el mensaje
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        // tras logout redirige aqui, MainController detecta
                        // el parametro logout y muestra confirmacion
                        .permitAll()
                );
        return http.build();
    }
}