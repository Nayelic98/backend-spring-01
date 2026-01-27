package ec.edu.ups.icc.fundamentos01.users.services;

import ec.edu.ups.icc.fundamentos01.categories.dtos.CategoryResponseDto;
import ec.edu.ups.icc.fundamentos01.categories.entity.CategoryEntity;
import ec.edu.ups.icc.fundamentos01.exceptions.domain.ConflictException;
import ec.edu.ups.icc.fundamentos01.exceptions.domain.NotFoundException;
import ec.edu.ups.icc.fundamentos01.products.dtos.ProductResponseDto;
import ec.edu.ups.icc.fundamentos01.products.models.ProductEntity;
import ec.edu.ups.icc.fundamentos01.products.repository.ProductRepository;
import ec.edu.ups.icc.fundamentos01.users.dtos.*;
import ec.edu.ups.icc.fundamentos01.users.models.UserEntity;
import ec.edu.ups.icc.fundamentos01.users.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public UserServiceImpl(UserRepository userRepository, ProductRepository productRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Override
    public List<UserResponseDto> findAll() {
        return userRepository.findAll().stream()
                .map(this::toResponseDto)
                .toList();
    }

    // CORRECCIÓN 1: Devolver UserResponseDto en lugar de Object
    @Override
    public UserResponseDto findOne(int id) {
        return userRepository.findById((long) id)
                .map(this::toResponseDto)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado con ID: " + id));
    }

    @Override
    public UserResponseDto create(CreateUserDto dto) {
        if (userRepository.findByEmail(dto.email).isPresent()) {
            throw new ConflictException("El email ya está registrado: " + dto.email);
        }

        UserEntity user = new UserEntity();
        user.setName(dto.name);
        user.setEmail(dto.email);
        user.setPassword(dto.password);

        UserEntity savedEntity = userRepository.save(user);
        return toResponseDto(savedEntity);
    }

    // CORRECCIÓN 2: Devolver UserResponseDto en lugar de Object
    @Override
    public UserResponseDto update(int id, UpdateUserDto dto) {
        UserEntity user = userRepository.findById((long) id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado con ID: " + id));

        user.setName(dto.name);
        user.setPassword(dto.password);

        return toResponseDto(userRepository.save(user));
    }

    // CORRECCIÓN 3: Devolver UserResponseDto en lugar de Object
    @Override
    public UserResponseDto partialUpdate(int id, PartialUpdateUserDto dto) {
        UserEntity user = userRepository.findById((long) id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado con ID: " + id));

        if (dto.name != null)
            user.setName(dto.name);
        if (dto.password != null)
            user.setPassword(dto.password);

        return toResponseDto(userRepository.save(user));
    }

    // CORRECCIÓN 4: Cambiar a void (seguramente así está en la interfaz) y quitar
    // el return
    @Override
    public void delete(int id) {
        if (!userRepository.existsById((long) id)) {
            throw new NotFoundException("No se puede eliminar. Usuario no encontrado con ID: " + id);
        }
        userRepository.deleteById((long) id);
    }

    // ================= MÉTODOS RELACIONADOS CON PRODUCTOS =================

    @Override
    public List<ProductResponseDto> getProductsByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Usuario no encontrado con ID: " + userId);
        }
        return productRepository.findByOwnerId(userId).stream()
                .map(this::mapProductToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getProductsByUserIdWithFilters(Long userId, String name, Double minPrice,
            Double maxPrice, Long categoryId) {

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Usuario no encontrado con ID: " + userId);
        }

        Page<ProductEntity> page = productRepository.findByUserIdWithFilters(
                userId, name, minPrice, maxPrice, categoryId, Pageable.unpaged());

        return page.getContent().stream()
                .map(this::mapProductToDto)
                .toList();
    }

    // ================= MAPPERS AUXILIARES =================

    private UserResponseDto toResponseDto(UserEntity entity) {
        UserResponseDto dto = new UserResponseDto();
        // CORRECCIÓN 5: Usar .intValue() para convertir el Long a int sin errores
        dto.id = entity.getId().intValue();
        dto.name = entity.getName();
        dto.email = entity.getEmail();
        return dto;
    }

    private ProductResponseDto mapProductToDto(ProductEntity entity) {
        ProductResponseDto dto = new ProductResponseDto();
        dto.id = entity.getId();
        dto.name = entity.getName();
        dto.price = entity.getPrice();
        dto.description = entity.getDescription();
        dto.createdAt = entity.getCreatedAt();
        dto.updatedAt = entity.getUpdatedAt();

        if (entity.getCategories() != null) {
            List<CategoryResponseDto> catDtos = new ArrayList<>();
            for (CategoryEntity cat : entity.getCategories()) {
                CategoryResponseDto c = new CategoryResponseDto();
                c.id = cat.getId();
                c.name = cat.getName();
                c.description = cat.getDescription();
                catDtos.add(c);
            }
            dto.categories = catDtos;
        }
        return dto;
    }
}