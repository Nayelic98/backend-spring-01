package ec.edu.ups.icc.fundamentos01.security.services;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ec.edu.ups.icc.fundamentos01.users.models.UserEntity;
import ec.edu.ups.icc.fundamentos01.users.repository.UserRepository;
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    /**
     * Repositorio para acceder a la base de datos
     * 
     * Inyectado por Spring automáticamente (constructor injection)
     */
    private final UserRepository userRepository;

    /**
     * Constructor: Spring inyecta UserRepository automáticamente
     * 
     * @param userRepository: Repositorio de usuarios
     */
    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * loadUserByUsername: MÉTODO PRINCIPAL de UserDetailsService
     * 
     * SecurityContext.setAuthentication(userDetails)
     * 
     * @param email: Email del usuario (lo llamamos username por el contrato)
     * @return UserDetails: Usuario convertido a formato Spring Security
     * @throws UsernameNotFoundException: Si el usuario no existe
     * 
     * @Transactional(readOnly = true):
     *                         - readOnly = true: Optimización para consultas SELECT
     *                         - Permite a Hibernate/PostgreSQL optimizar la query
     *                         - NO permite operaciones de escritura (INSERT,
     *                         UPDATE, DELETE)
     *                         - Si intentamos modificar, lanza excepción
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        /**
         * 1. Buscar usuario por email en la base de datos
         * 
         * Nota: Los roles se cargan automáticamente por FetchType.EAGER
         */
        UserEntity user = userRepository.findByEmail(email)
                /**
                 * .orElseThrow(): Si Optional está vacío, lanza excepción
                 */
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado con email: " + email));

        /**
         * 2. Convertir UserEntity → UserDetailsImpl
         * 
         * UserDetailsImpl.build(user):
         * - Factory method que convierte nuestro UserEntity
         * - Extrae roles y los convierte a authorities
         * - Retorna objeto compatible con Spring Security
         * 
         */
        return UserDetailsImpl.build(user);
    }
}
