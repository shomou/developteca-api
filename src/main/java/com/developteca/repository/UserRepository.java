package com.developteca.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.developteca.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long>{
    /**
     * Buscar un usuario por email
     * @param email el email del usuario
     * @return Optional<User> con el usuario si existe
     */
    Optional<User> findByEmail(String email);

    /**
     * Verificar si existe un usuario con ese email
     * @param email el email a verificar
     * @return true si existe, false si no.
     */
    boolean existsByEmail(String email);

    Optional<User> findByEmailVerificationToken(String token);
}
