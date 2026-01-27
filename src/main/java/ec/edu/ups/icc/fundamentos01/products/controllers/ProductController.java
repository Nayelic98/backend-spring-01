package ec.edu.ups.icc.fundamentos01.products.controllers;

import ec.edu.ups.icc.fundamentos01.products.dtos.CreateProductDto;
import ec.edu.ups.icc.fundamentos01.products.dtos.ProductResponseDto;
import ec.edu.ups.icc.fundamentos01.products.dtos.UpdateProductDto;
import ec.edu.ups.icc.fundamentos01.products.services.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import ec.edu.ups.icc.fundamentos01.security.services.UserDetailsImpl;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

        private final ProductService productService;

        public ProductController(ProductService productService) {
                this.productService = productService;
        }

        @GetMapping
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<List<ProductResponseDto>> findAll() {
                List<ProductResponseDto> products = productService.findAllList();
                return ResponseEntity.ok(products);
        }

        // 1. ENDPOINT PAGE (Paginación normal con totales)
        @GetMapping("/paginated")
        public ResponseEntity<Page<ProductResponseDto>> getAllProducts(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "id,asc") String[] sort

        ) {
                return ResponseEntity.ok(productService.findAll(page, size, sort));
        }

        // 2. ENDPOINT SLICE (Paginación ligera para rendimiento)
        // ESTE ES EL QUE TE ESTÁ FALLANDO. Asegúrate que llame a findAllSlice
        @GetMapping("/slice")
        public ResponseEntity<Slice<ProductResponseDto>> getProductsSlice(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "id,asc") String[] sort) {

                return ResponseEntity.ok(productService.findAllSlice(page, size, sort));
        }

        // 3. ENDPOINT SEARCH (Buscador con filtros)
        @GetMapping("/search")
        public ResponseEntity<Page<ProductResponseDto>> searchProducts(
                        @RequestParam(required = false) String name,
                        @RequestParam(required = false) Double minPrice,
                        @RequestParam(required = false) Double maxPrice,
                        @RequestParam(required = false) Long categoryId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "id,asc") String[] sort) {
                return ResponseEntity.ok(
                                productService.findWithFilters(name, minPrice, maxPrice, categoryId, page, size, sort));
        }

        // 4. ENDPOINT POR USUARIO (Con filtros)
        @GetMapping("/user/{userId}")
        public ResponseEntity<Page<ProductResponseDto>> getProductsByUser(
                        @PathVariable Long userId,
                        @RequestParam(required = false) String name,
                        @RequestParam(required = false) Double minPrice,
                        @RequestParam(required = false) Double maxPrice,
                        @RequestParam(required = false) Long categoryId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "id,asc") String[] sort) {
                return ResponseEntity.ok(productService.findByUserIdWithFilters(userId, name, minPrice, maxPrice,
                                categoryId, page, size, sort));
        }

        @PostMapping
        public ResponseEntity<ProductResponseDto> create(@RequestBody CreateProductDto dto) {
                return ResponseEntity.status(201).body(productService.create(dto));
        }

        @GetMapping("/{id}")
        public ResponseEntity<ProductResponseDto> getById(@PathVariable Long id) {
                return ResponseEntity.ok(productService.findById(id));
        }

        @PutMapping("/{id}")
        public ResponseEntity<ProductResponseDto> update(
                        @PathVariable Long id,
                        @Valid @RequestBody UpdateProductDto dto,
                        @AuthenticationPrincipal UserDetailsImpl currentUser) {

                ProductResponseDto updated = productService.update(id, dto, currentUser);
                return ResponseEntity.ok(updated);
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> delete(
                        @PathVariable Long id,
                        @AuthenticationPrincipal UserDetailsImpl currentUser) {
                productService.delete(id, currentUser);
                return ResponseEntity.noContent().build();
        }

        @GetMapping("/list")
        public ResponseEntity<List<ProductResponseDto>> getAllList() {
                return ResponseEntity.ok(productService.findAllList());
        }

}