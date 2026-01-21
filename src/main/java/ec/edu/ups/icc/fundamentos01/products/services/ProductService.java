package ec.edu.ups.icc.fundamentos01.products.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;

import ec.edu.ups.icc.fundamentos01.products.dtos.CreateProductDto;

import ec.edu.ups.icc.fundamentos01.products.dtos.UpdateProductDto;
import ec.edu.ups.icc.fundamentos01.products.dtos.ProductResponseDto;

public interface ProductService {

    ProductResponseDto create(CreateProductDto dto);

    Page<ProductResponseDto> findAllPaginado(int page, int size, String[] sort);
    List<ProductResponseDto> findAll();

    ProductResponseDto findById(Long id);

    List<ProductResponseDto> findByUserId(Long id);

    //List<ProductResponseDto> findByCategoryId(Long id);

    ProductResponseDto update(Long id, UpdateProductDto dto);
    Slice<ProductResponseDto> findAllSlice(int page, int size, String[] sort);
    Page<ProductResponseDto> findWithFilters(
        String name, 
        Double minPrice, 
        Double maxPrice, 
        Long categoryId,
        int page, 
        int size, 
        String[] sort
    );
    Page<ProductResponseDto> findByUserIdWithFilters(
        Long userId,
        String name,
        Double minPrice,
        Double maxPrice,
        Long categoryId,
        int page,
        int size,
        String[] sort
    );
    void delete(Long id);

}
