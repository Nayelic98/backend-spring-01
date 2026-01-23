package ec.edu.ups.icc.fundamentos01.security.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ec.edu.ups.icc.fundamentos01.security.models.RoleEntity;
import ec.edu.ups.icc.fundamentos01.security.models.RoleName;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    // Buscar rol por nombre (ROLE_USER, ROLE_ADMIN, etc.)
    Optional<RoleEntity> findByName(RoleName name);
    
    // Verificar si existe un rol específico
    boolean existsByName(RoleName name);
}