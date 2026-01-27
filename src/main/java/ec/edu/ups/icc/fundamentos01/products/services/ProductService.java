package ec.edu.ups.icc.fundamentos01.products.services;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import ec.edu.ups.icc.fundamentos01.products.dtos.CreateProductDto;
import ec.edu.ups.icc.fundamentos01.products.dtos.UpdateProductDto;
import ec.edu.ups.icc.fundamentos01.security.services.UserDetailsImpl;
import ec.edu.ups.icc.fundamentos01.products.dtos.ProductResponseDto;

public interface ProductService {

        ProductResponseDto create(CreateProductDto dto);

        Page<ProductResponseDto> findAll(int page, int size, String[] sort);

        Slice<ProductResponseDto> findAllSlice(int page, int size, String[] sort);

        List<ProductResponseDto> findAllList();

        ProductResponseDto findById(Long id);

        Page<ProductResponseDto> findWithFilters(
                        String name, Double minPrice, Double maxPrice, Long categoryId,
                        int page, int size, String[] sort);

        Page<ProductResponseDto> findByUserIdWithFilters(
                        Long userId, String name, Double minPrice, Double maxPrice, Long categoryId,
                        int page, int size, String[] sort);

        List<ProductResponseDto> findByUserId(Long id);

        List<ProductResponseDto> findByCategoryId(Long id);

        ProductResponseDto update(Long id, UpdateProductDto dto, UserDetailsImpl currentUser);

        void delete(Long id, UserDetailsImpl currentUser);
}