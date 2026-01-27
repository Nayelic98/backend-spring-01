package ec.edu.ups.icc.fundamentos01.products.repository;

import ec.edu.ups.icc.fundamentos01.products.models.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

        Optional<ProductEntity> findByName(String name);

        Slice<ProductEntity> findBy(Pageable pageable);

        List<ProductEntity> findByOwnerId(Long userId);

        List<ProductEntity> findByCategoriesId(Long categoryId);

        // ============== CONSULTAS CON FILTROS Y PAGINACIÓN ==============

        /**
         * Busca productos con filtros opcionales y paginación.
         */
        @Query(value = "SELECT DISTINCT p FROM ProductEntity p " +
                        "LEFT JOIN p.categories c " +
                        "WHERE (COALESCE(:name, '') = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
                        "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
                        "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
                        "AND (:categoryId IS NULL OR c.id = :categoryId)",

                        countQuery = "SELECT count(DISTINCT p) FROM ProductEntity p " +
                                        "LEFT JOIN p.categories c " +
                                        "WHERE (COALESCE(:name, '') = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) "
                                        +
                                        "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
                                        "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
                                        "AND (:categoryId IS NULL OR c.id = :categoryId)")
        Page<ProductEntity> findWithFilters(
                        @Param("name") String name,
                        @Param("minPrice") Double minPrice,
                        @Param("maxPrice") Double maxPrice,
                        @Param("categoryId") Long categoryId,
                        Pageable pageable);

        /**
         * Busca productos DE UN USUARIO con filtros opcionales y paginación.
         */
        @Query("SELECT DISTINCT p FROM ProductEntity p " +
                        "LEFT JOIN p.categories c " +
                        "WHERE p.owner.id = :userId " +
                        "AND (COALESCE(:name, '') = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
                        "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
                        "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
                        "AND (:categoryId IS NULL OR c.id = :categoryId)")
        Page<ProductEntity> findByUserIdWithFilters(
                        @Param("userId") Long userId,
                        @Param("name") String name,
                        @Param("minPrice") Double minPrice,
                        @Param("maxPrice") Double maxPrice,
                        @Param("categoryId") Long categoryId,
                        Pageable pageable);
}