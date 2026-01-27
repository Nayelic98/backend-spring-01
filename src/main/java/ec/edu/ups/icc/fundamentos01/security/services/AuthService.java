package ec.edu.ups.icc.fundamentos01.security.services;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ec.edu.ups.icc.fundamentos01.exceptions.domain.BadRequestException;
import ec.edu.ups.icc.fundamentos01.exceptions.domain.ConflictException;
import ec.edu.ups.icc.fundamentos01.security.dtos.AuthResponseDto;
import ec.edu.ups.icc.fundamentos01.security.dtos.LoginRequestDto;
import ec.edu.ups.icc.fundamentos01.security.dtos.RegisterRequestDto;
import ec.edu.ups.icc.fundamentos01.security.models.RoleEntity;
import ec.edu.ups.icc.fundamentos01.security.models.RoleName;
import ec.edu.ups.icc.fundamentos01.security.repository.RoleRepository;
import ec.edu.ups.icc.fundamentos01.security.utils.JwtUtil;
import ec.edu.ups.icc.fundamentos01.users.models.UserEntity;
import ec.edu.ups.icc.fundamentos01.users.repository.UserRepository;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

        private final AuthenticationManager authenticationManager;
        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtUtil jwtUtil;

        public AuthService(AuthenticationManager authenticationManager,
                        UserRepository userRepository,
                        RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder,
                        JwtUtil jwtUtil) {
                this.authenticationManager = authenticationManager;
                this.userRepository = userRepository;
                this.roleRepository = roleRepository;
                this.passwordEncoder = passwordEncoder;
                this.jwtUtil = jwtUtil;
        }

        @Transactional(readOnly = true)
        public AuthResponseDto login(LoginRequestDto loginRequest) {
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                loginRequest.getEmail(),
                                                loginRequest.getPassword()));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                String jwt = jwtUtil.generateToken(authentication);
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

                Set<String> roles = userDetails.getAuthorities().stream()
                                .map(item -> item.getAuthority())
                                .collect(Collectors.toSet());
                return new AuthResponseDto(
                                jwt,
                                userDetails.getId(),
                                userDetails.getName(),
                                userDetails.getEmail(),
                                roles);
        }

        @Transactional
        public AuthResponseDto register(RegisterRequestDto registerRequest) {
                if (userRepository.existsByEmail(registerRequest.getEmail())) {
                        throw new ConflictException("El email ya está registrado");
                }
                UserEntity user = new UserEntity();
                user.setName(registerRequest.getName());
                user.setEmail(registerRequest.getEmail());

                user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
                RoleEntity userRole = roleRepository.findByName(RoleName.ROLE_USER)
                                .orElseThrow(() -> new BadRequestException("Rol por defecto no encontrado"));

                Set<RoleEntity> roles = new HashSet<>();
                roles.add(userRole);
                user.setRoles(roles);
                user = userRepository.save(user);
                UserDetailsImpl userDetails = UserDetailsImpl.build(user);
                String jwt = jwtUtil.generateTokenFromUserDetails(userDetails);
                Set<String> roleNames = user.getRoles().stream()
                                .map(role -> role.getName().name())
                                .collect(Collectors.toSet());
                return new AuthResponseDto(
                                jwt,
                                user.getId(),
                                user.getName(),
                                user.getEmail(),
                                roleNames);
        }

}
