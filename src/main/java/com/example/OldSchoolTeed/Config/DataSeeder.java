package com.example.OldSchoolTeed.Config;



import com.example.OldSchoolTeed.entities.Rol;
import com.example.OldSchoolTeed.entities.Usuario;
import com.example.OldSchoolTeed.repository.RolRepository;
import com.example.OldSchoolTeed.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UsuarioRepository usuarioRepository, RolRepository rolRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1. Crear roles si no existen
        Rol rolCliente = rolRepository.findByNombre("Cliente")
                .orElseGet(() -> rolRepository.save(new Rol("Cliente")));

        Rol rolAdmin = rolRepository.findByNombre("Administrador")
                .orElseGet(() -> rolRepository.save(new Rol("Administrador")));

        // 2. Crear el usuario Administrador si no existe
        String adminEmail = "admin@oldschool.com";
        if (!usuarioRepository.existsByEmail(adminEmail)) {
            Usuario adminUser = new Usuario();
            adminUser.setNombre("Admin OldSchool");
            adminUser.setEmail(adminEmail);
            // ¡Contraseña simple solo para desarrollo!
            adminUser.setPasswordHash(passwordEncoder.encode("admin123"));
            // Le damos ambos roles
            adminUser.setRoles(Set.of(rolAdmin, rolCliente));
            adminUser.setActivo(true);

            usuarioRepository.save(adminUser);

            // Mensaje para que sepas que se creó
            System.out.println("************************************************************");
            System.out.println(">>> Usuario Administrador creado con éxito:");
            System.out.println(">>> Email: " + adminEmail);
            System.out.println(">>> Pass:  admin123");
            System.out.println("************************************************************");
        }
    }
}