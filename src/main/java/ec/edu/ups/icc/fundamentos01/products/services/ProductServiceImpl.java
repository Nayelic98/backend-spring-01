package ec.edu.ups.icc.fundamentos01.products.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import ec.edu.ups.icc.fundamentos01.categories.dtos.CategoryResponseDto;
import ec.edu.ups.icc.fundamentos01.categories.entity.CategoryEntity;
import ec.edu.ups.icc.fundamentos01.categories.reporitory.CategoryRepository;
import ec.edu.ups.icc.fundamentos01.exceptions.domain.BadRequestException;
import ec.edu.ups.icc.fundamentos01.exceptions.domain.NotFoundException;
import ec.edu.ups.icc.fundamentos01.products.dtos.CreateProductDto;
import ec.edu.ups.icc.fundamentos01.products.dtos.ProductResponseDto;
import ec.edu.ups.icc.fundamentos01.products.dtos.UpdateProductDto;
import ec.edu.ups.icc.fundamentos01.products.models.Product;
import ec.edu.ups.icc.fundamentos01.products.models.ProductEntity;
import ec.edu.ups.icc.fundamentos01.products.repository.ProductRepository;
import ec.edu.ups.icc.fundamentos01.security.services.UserDetailsImpl;
import ec.edu.ups.icc.fundamentos01.users.models.UserEntity;
import ec.edu.ups.icc.fundamentos01.users.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepo;
    private final UserRepository userRepo;
    private final CategoryRepository categoryRepo;

    public ProductServiceImpl(ProductRepository productRepo, UserRepository userRepo,
            CategoryRepository categoryRepository) {
        this.productRepo = productRepo;
        this.userRepo = userRepo;
        this.categoryRepo = categoryRepository;
    }

    // ================== MÉTODOS PAGINADOS (LA NOVEDAD) ==================

    @Override
    public Page<ProductResponseDto> findAll(int page, int size, String[] sort) {
        Pageable pageable = createPageable(page, size, sort);
        Page<ProductEntity> productPage = productRepo.findAll(pageable);
        return productPage.map(this::toResponseDto);
    }

    @Override
    public Slice<ProductResponseDto> findAllSlice(int page, int size, String[] sort) {
        Pageable pageable = createPageable(page, size, sort);
        Slice<ProductEntity> productSlice = productRepo.findBy(pageable);
        return productSlice.map(this::toResponseDto);
    }

    @Override
    public Page<ProductResponseDto> findWithFilters(String name, Double minPrice, Double maxPrice, Long categoryId,
            int page, int size, String[] sort) {
        validateFilterParameters(minPrice, maxPrice);
        Pageable pageable = createPageable(page, size, sort);

        Page<ProductEntity> productPage = productRepo.findWithFilters(name, minPrice, maxPrice, categoryId, pageable);
        return productPage.map(this::toResponseDto);
    }

    @Override
    public Page<ProductResponseDto> findByUserIdWithFilters(Long userId, String name, Double minPrice, Double maxPrice,
            Long categoryId, int page, int size, String[] sort) {
        if (!userRepo.existsById(userId)) {
            throw new NotFoundException("Usuario no encontrado con ID: " + userId);
        }
        validateFilterParameters(minPrice, maxPrice);
        Pageable pageable = createPageable(page, size, sort);
        Page<ProductEntity> productPage = productRepo.findByUserIdWithFilters(userId, name, minPrice, maxPrice,
                categoryId, pageable);
        return productPage.map(this::toResponseDto);
    }

    // ================== MÉTODOS CRUD ESTÁNDAR ==================

    @Override
    public ProductResponseDto create(CreateProductDto dto) {
        UserEntity owner = userRepo.findById(dto.userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado con ID: " + dto.userId));
        Set<CategoryEntity> categories = validateAndGetCategories(dto.categoryIds);

        if (productRepo.findByName(dto.name).isPresent()) {
            throw new IllegalStateException("El nombre del producto ya está registrado");
        }

        Product product = Product.fromDto(dto);
        ProductEntity entity = product.toEntity(owner, categories);
        ProductEntity saved = productRepo.save(entity);
        return toResponseDto(saved);
    }

    @Override
    public List<ProductResponseDto> findAllList() {
        return productRepo.findAll().stream().map(this::toResponseDto).toList();
    }

    @Override
    public ProductResponseDto findById(Long id) {
        return productRepo.findById(id)
                .map(this::toResponseDto)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado con ID: " + id));
    }

    @Override
    public List<ProductResponseDto> findByUserId(Long userId) {
        if (!userRepo.existsById(userId))
            throw new NotFoundException("Usuario no encontrado");
        return productRepo.findByOwnerId(userId).stream().map(this::toResponseDto).toList();
    }

    @Override
    public List<ProductResponseDto> findByCategoryId(Long categoryId) {
        if (!categoryRepo.existsById(categoryId))
            throw new NotFoundException("Categoría no encontrada");
        return productRepo.findByCategoriesId(categoryId).stream().map(this::toResponseDto).toList();
    }

    @Override
    public ProductResponseDto update(Long id, UpdateProductDto dto, UserDetailsImpl currentUser) {
        ProductEntity existing = productRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado con ID: " + id));

        validateOwnership(existing, currentUser);

        Set<CategoryEntity> categories = validateAndGetCategories(dto.categoryIds);
        Product product = Product.fromEntity(existing);
        product.update(dto);

        ProductEntity updated = product.toEntity(existing.getOwner(), categories);
        updated.setId(id);

        ProductEntity saved = productRepo.save(updated);
        return toResponseDto(saved);
    }

    @Override
    public void delete(Long id, UserDetailsImpl currentUser) {
        ProductEntity product = productRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado con ID: " + id));

        validateOwnership(product, currentUser);

        productRepo.deleteById(id);
    }

    // ================== HELPERS Y VALIDACIONES ==================

    private Pageable createPageable(int page, int size, String[] sort) {
        if (page < 0)
            throw new BadRequestException("La página debe ser mayor o igual a 0");
        if (size < 1 || size > 100)
            throw new BadRequestException("El tamaño debe estar entre 1 y 100");
        Sort sortObj = createSort(sort);
        return PageRequest.of(page, size, sortObj);
    }

    private Sort createSort(String[] sort) {
        if (sort == null || sort.length == 0) {
            return Sort.by("id");
        }
        if (sort.length == 2 && !sort[0].contains(",")
                && (sort[1].equalsIgnoreCase("asc") || sort[1].equalsIgnoreCase("desc"))) {
            String property = sort[0];
            String direction = sort[1];
            if (!isValidSortProperty(property)) {
                throw new BadRequestException("Propiedad de ordenamiento no válida: " + property);
            }
            return "desc".equalsIgnoreCase(direction) ? Sort.by(Sort.Order.desc(property))
                    : Sort.by(Sort.Order.asc(property));
        }

        List<Sort.Order> orders = new ArrayList<>();
        for (String sortParam : sort) {
            String[] parts = sortParam.split(",");
            String property = parts[0];
            String direction = parts.length > 1 ? parts[1] : "asc";

            if (!isValidSortProperty(property)) {
                throw new BadRequestException("Propiedad de ordenamiento no válida: " + property);
            }

            Sort.Order order = "desc".equalsIgnoreCase(direction)
                    ? Sort.Order.desc(property)
                    : Sort.Order.asc(property);

            orders.add(order);
        }

        return Sort.by(orders);
    }

    private boolean isValidSortProperty(String property) {
        Set<String> allowedProperties = Set.of("id", "name", "price", "createdAt", "updatedAt", "owner.name",
                "owner.email");
        return allowedProperties.contains(property);
    }

    private void validateFilterParameters(Double minPrice, Double maxPrice) {
        if (minPrice != null && minPrice < 0)
            throw new BadRequestException("El precio mínimo no puede ser negativo");
        if (maxPrice != null && maxPrice < 0)
            throw new BadRequestException("El precio máximo no puede ser negativo");
        if (minPrice != null && maxPrice != null && maxPrice < minPrice)
            throw new BadRequestException("El precio máximo debe ser mayor al mínimo");
    }

    private Set<CategoryEntity> validateAndGetCategories(Set<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return new HashSet<>();
        }
        List<CategoryEntity> categoriesFound = categoryRepo.findAllById(categoryIds);

        if (categoriesFound.size() != categoryIds.size()) {
            throw new NotFoundException("Alguna de las categorías no existe");
        }
        return new HashSet<>(categoriesFound);
    }

    private ProductResponseDto toResponseDto(ProductEntity entity) {
        ProductResponseDto dto = new ProductResponseDto();
        dto.id = entity.getId();
        dto.name = entity.getName();
        dto.price = entity.getPrice();
        dto.description = entity.getDescription();
        dto.createdAt = entity.getCreatedAt();
        dto.updatedAt = entity.getUpdatedAt();

        ProductResponseDto.UserSummaryDto ownerDto = new ProductResponseDto.UserSummaryDto();
        ownerDto.id = entity.getOwner().getId();
        ownerDto.name = entity.getOwner().getName();
        ownerDto.email = entity.getOwner().getEmail();

        List<CategoryResponseDto> categoryDtos = new ArrayList<>();
        if (entity.getCategories() != null) {
            for (CategoryEntity categoryEntity : entity.getCategories()) {
                CategoryResponseDto categoryDto = new CategoryResponseDto();
                categoryDto.id = categoryEntity.getId();
                categoryDto.name = categoryEntity.getName();
                categoryDtos.add(categoryDto);
            }
        }
        dto.user = ownerDto;
        dto.categories = categoryDtos;
        return dto;
    }

    private void validateOwnership(ProductEntity product, UserDetailsImpl currentUser) {

        if (hasAnyRole(currentUser, "ROLE_ADMIN", "ROLE_MODERATOR")) {
            return;
        }

        if (!product.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("No puedes modificar productos ajenos");
        }
    }

    private boolean hasAnyRole(UserDetailsImpl user, String... roles) {
        for (String role : roles) {
            for (GrantedAuthority authority : user.getAuthorities()) {
                if (authority.getAuthority().equals(role)) {
                    return true;
                }
            }
        }
        return false;
    }

}