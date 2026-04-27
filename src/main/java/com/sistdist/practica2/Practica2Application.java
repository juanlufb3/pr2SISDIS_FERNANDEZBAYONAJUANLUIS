package com.sistdist.practica2;

import com.sistdist.practica2.model.Role;
import com.sistdist.practica2.model.User;
import com.sistdist.practica2.repository.RoleRepository;
import com.sistdist.practica2.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class Practica2Application {

	public static void main(String[] args) {
		SpringApplication.run(Practica2Application.class, args);
	}

	// Crea usuarios de prueba automáticamente al arrancar
	@Bean
	CommandLineRunner initData(UserRepository userRepo,
	                           RoleRepository roleRepo,
	                           PasswordEncoder encoder) {
		return args -> {
			if (roleRepo.count() == 0) {
				Role admin = new Role(null, "ROLE_ADMIN", 1);
				Role user  = new Role(null, "ROLE_USER",  1);
				roleRepo.save(admin);
				roleRepo.save(user);

				User adminUser = new User();
				adminUser.setUsername("admin");
				adminUser.setEmail("admin@sisdis.com");
				adminUser.setPassword(encoder.encode("admin123"));
				adminUser.setUserRole(admin);
				userRepo.save(adminUser);

				User normalUser = new User();
				normalUser.setUsername("usuario");
				normalUser.setEmail("usuario@sisdis.com");
				normalUser.setPassword(encoder.encode("user123"));
				normalUser.setUserRole(user);
				userRepo.save(normalUser);

				System.out.println("✅ Usuarios creados: admin/admin123 | usuario/user123");
			}
		};
	}
}