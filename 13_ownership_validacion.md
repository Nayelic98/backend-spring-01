# Programación y Plataformas Web

# **Spring Boot – Validación de Ownership**

<div align="center">
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/spring/spring-original.svg" width="95">
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/postgresql/postgresql-original.svg" width="95">
</div>

## **Práctica 13 (Spring Boot): Validación de Propiedad de Recursos**

### **Autores**

**Pablo Torres**

📧 [ptorresp@ups.edu.ec](mailto:ptorresp@ups.edu.ec)

💻 GitHub: PabloT18


# **Introducción**

En las prácticas anteriores implementamos:
- **Práctica 11**: Autenticación con JWT → Valida que el usuario esté autenticado (401)
- **Práctica 12**: Autorización por roles → Valida que tenga el rol correcto (403)

Ahora implementaremos **validación de ownership (propiedad)**: Validar que un usuario solo pueda modificar/eliminar **sus propios recursos**.

**Escenario del problema**:
```
Usuario A (ROLE_USER) crea Producto #1 → owner_id = A
Usuario B (ROLE_USER) intenta eliminar Producto #1 → ¿Debería poder?
→ NO, solo el dueño (A) o un ADMIN pueden eliminarlo
```

**En esta práctica aprenderás**:
- Validación de ownership en servicios
- Método `validateOwnership()` reutilizable
- Diferencia entre validación por rol y por propiedad
- ADMIN y MODERATOR pueden saltarse validación de ownership
- Manejo automático de excepciones con `AccessDeniedException`


# **1. Concepto: ¿Qué es Ownership?**

**Ownership (Propiedad)** significa que un recurso pertenece a un usuario específico. Solo el dueño (o usuarios con permisos especiales) pueden modificarlo o eliminarlo.

## **1.1. Ejemplos de Ownership**

| Recurso | Owner | Quién puede modificar/eliminar |
|---------|-------|--------------------------------|
| Producto #1 | Usuario A | Usuario A, ADMIN, MODERATOR |
| Producto #2 | Usuario B | Usuario B, ADMIN, MODERATOR |
| Comentario #5 | Usuario C | Usuario C, ADMIN, MODERATOR |
| Orden #10 | Usuario D | Usuario D, ADMIN |

## **1.2. Flujo de Validación**

```
Request: DELETE /api/products/1
Header: Authorization: Bearer <token-Usuario-B>
        ↓
1. JwtAuthenticationFilter valida token → OK (Usuario B autenticado)
2. .anyRequest().authenticated() → OK (Usuario B autenticado)
3. Servicio: productService.delete(1)
   → Busca Producto #1 → owner_id = Usuario A
   → Usuario actual = Usuario B
   → validateOwnership(producto)
      - ¿Usuario B es ADMIN? → NO
      - ¿Usuario B es MODERATOR? → NO
      - ¿Usuario B es dueño? → NO (dueño es Usuario A)
      → Lanza AccessDeniedException
        ↓
4. GlobalExceptionHandler captura AccessDeniedException
   → Devuelve 403 Forbidden con mensaje personalizado
        ↓
Response: 403 Forbidden
{
  "status": 403,
  "error": "Forbidden",
  "message": "No puedes modificar productos ajenos"
}
```


# **2. Implementación del Controlador**

## **2.1. Extraer Usuario Autenticado en el Controlador**

El controlador debe extraer el usuario del JWT y pasarlo al servicio como parámetro.

Archivo: `products/controllers/ProductController.java`

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // ... otros endpoints (GET, POST, etc.)

    /**
     * Actualizar producto (solo dueño, ADMIN o MODERATOR)
     * 
     * El usuario autenticado se extrae del JWT mediante @AuthenticationPrincipal
     * y se pasa al servicio para validar ownership
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductDto dto,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {  // ← Usuario del JWT
        
        ProductResponseDto updated = productService.update(id, dto, currentUser);
        return ResponseEntity.ok(updated);
    }

    /**
     * Eliminar producto (solo dueño, ADMIN o MODERATOR)
     * 
     * El usuario autenticado se extrae del JWT mediante @AuthenticationPrincipal
     * y se pasa al servicio para validar ownership
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {  // ← Usuario del JWT
        
        productService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
```

**Ventajas de este enfoque**:
- **Más explícito**: Se ve claramente que el servicio necesita el usuario
- **Más testeable**: No depende de `SecurityContextHolder` estático
- **Menos acoplamiento**: El servicio no depende de Spring Security directamente
- **Más claro**: En el controlador se ve de dónde viene el usuario


# **3. Implementación en ProductService**

## **3.1. Estructura del Servicio**

Archivo: `products/services/ProductService.java`

```java


public class ProductService {

    // ============== MÉTODOS DE CONSULTA ==============


    // ============== MÉTODOS DE MODIFICACIÓN CON VALIDACIÓN DE OWNERSHIP ==============

    /**
     * Actualizar producto con validación de ownership
     * 
     * Validación:
     * - Si eres ADMIN o MODERATOR → Puedes actualizar cualquier producto
     * - Si eres USER → Solo puedes actualizar TUS productos
     * 
     * @param id ID del producto a actualizar
     * @param dto Datos para actualizar
     * @param currentUser Usuario autenticado (del JWT)
     * @throws AccessDeniedException si no eres dueño ni tienes rol privilegiado
     */
    @Transactional
    public ProductResponseDto update(Long id, UpdateProductDto dto, UserDetailsImpl currentUser) {


        // 1. BUSCAR PRODUCTO EXISTENTE

        
        // 2. VALIDACIÓN DE OWNERSHIP (pasando el usuario)
        validateOwnership(product, currentUser);

        // Si pasa la validación, actualizar
        // 3. VALIDAR Y OBTENER CATEGORÍAS

        // 4. ACTUALIZAR USANDO DOMINIO

        // 5. CONVERTIR A ENTIDAD MANTENIENDO OWNER ORIGINAL
        
        // 6. PERSISTIR Y RESPONDER
        ProductEntity saved = productRepo.save(updated);

        return productMapper.toDto(updated);
    }

    /**
     * Eliminar producto con validación de ownership
     * 
     * Validación:
     * - Si eres ADMIN o MODERATOR → Puedes eliminar cualquier producto
     * - Si eres USER → Solo puedes eliminar TUS productos
     * 
     * @param id ID del producto a eliminar
     * @param currentUser Usuario autenticado (del JWT)
     * @throws AccessDeniedException si no eres dueño ni tienes rol privilegiado
     */
    @Transactional
    public void delete(Long id, UserDetailsImpl currentUser) {
        ProductEntity product = findProductOrThrow(id);
        
        // ← VALIDACIÓN DE OWNERSHIP (pasando el usuario)
        validateOwnership(product, currentUser);

        // Si pasa la validación, eliminar
        productRepository.delete(product);
    }

    // ============== MÉTODOS DE VALIDACIÓN Y UTILIDADES ==============

    /**
     * Valida si el usuario puede modificar/eliminar el producto
     * 
     * Lógica:
     * 1. Si tiene ROLE_ADMIN → Puede modificar cualquier producto
     * 2. Si tiene ROLE_MODERATOR → Puede modificar cualquier producto
     * 3. Si es ROLE_USER → Solo puede modificar sus propios productos
     * 
     * @param product Producto a validar
     * @param currentUser Usuario autenticado (del JWT)
     * @throws AccessDeniedException si no tiene permisos
     */
    private void validateOwnership(ProductEntity product, UserDetailsImpl currentUser) {
        // ADMIN y MODERATOR pueden modificar cualquier producto
        if (hasAnyRole(currentUser, "ROLE_ADMIN", "ROLE_MODERATOR")) {
            return;  // ← Pasa la validación automáticamente
        }

        // USER solo puede modificar sus propios productos
        if (!product.getOwner().getId().equals(currentUser.getId())) {
            // ← Lanza excepción que será capturada por GlobalExceptionHandler
            throw new AccessDeniedException("No puedes modificar productos ajenos");
        }

        // Si llega aquí, es el dueño → Pasa la validación
    }

    /**
     * Verifica si el usuario tiene alguno de los roles especificados
     * 
     * @param user Usuario a verificar
     * @param roles Roles a buscar
     * @return true si tiene al menos uno de los roles
     */
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

 

    /**
     * Crea Pageable con ordenamiento dinámico
     */
    private Pageable createPageable(int page, int size, String[] sort) {
        String sortField = sort[0];
        Sort.Direction sortDirection = sort.length > 1 && sort[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(page, size, Sort.by(sortDirection, sortField));
    }
}
```

## **3.2. Flujo de validateOwnership()**

```
Controlador: @AuthenticationPrincipal UserDetailsImpl currentUser
        ↓
Servicio: validateOwnership(product, currentUser)
        ↓
1. ¿Tiene ROLE_ADMIN?
   → SÍ → return (pasa validación)
   → NO → Continuar
        ↓
3. ¿Tiene ROLE_MODERATOR?
   → SÍ → return (pasa validación)
   → NO → Continuar
        ↓
4. ¿Es el dueño? (product.owner.id == currentUser.id)
   → SÍ → return (pasa validación)
   → NO → throw AccessDeniedException("No puedes modificar productos ajenos")
        ↓
5. GlobalExceptionHandler captura AccessDeniedException
   → Devuelve 403 con mensaje personalizado
```


# **4. Manejo Automático de Excepciones**

## **4.1. La Excepción AccessDeniedException**

Cuando un usuario intenta modificar un producto que no le pertenece y no tiene rol privilegiado, el servicio lanza:

```java
throw new AccessDeniedException("No puedes modificar productos ajenos");
```

**Esta excepción YA está manejada** en el `GlobalExceptionHandler` (Práctica 12):

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * Maneja AccessDeniedException (Spring Security legacy)
     * 
     * Se lanza desde:
     * - Validación de ownership en servicios
     * - Configuraciones de seguridad antiguas
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.FORBIDDEN,
                "Acceso denegado. No tienes los permisos necesarios",
                request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }
}
```


## **4.2. Personalizar el Mensaje por Contexto**

Si quieres que el mensaje sea más específico, puedes usar el mensaje de la excepción:

```java
@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<ErrorResponse> handleAccessDeniedException(
        AccessDeniedException ex,
        HttpServletRequest request) {
    ErrorResponse response = new ErrorResponse(
            HttpStatus.FORBIDDEN,
            ex.getMessage(),  // ← Usa el mensaje personalizado de la excepción
            request.getRequestURI());

    return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(response);
}
```

**Ahora la respuesta será**:

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "No puedes modificar productos ajenos",  ← Mensaje específico
  "path": "/api/products/1"
}
```


# **5. Ejemplos de Peticiones**

## **5.1. Usuario USER intenta modificar su propio producto (PERMITIDO)**

```http
# Usuario A es dueño del Producto #1
PUT http://localhost:8080/api/products/1
Authorization: Bearer <token-Usuario-A>
Body:
{
  "name": "Laptop Actualizada",
  "price": 1200
}

→ 200 OK
{
  "id": 1,
  "name": "Laptop Actualizada",
  "price": 1200,
  "owner": {
    "id": 1,
    "name": "Usuario A"
  }
}
```

## **5.2. Usuario USER intenta modificar producto ajeno (DENEGADO)**

```http
# Usuario B intenta modificar Producto #1 (dueño es Usuario A)
PUT http://localhost:8080/api/products/1
Authorization: Bearer <token-Usuario-B>
Body:
{
  "name": "Intento de modificar",
  "price": 1500
}

→ 403 Forbidden
{
  "status": 403,
  "error": "Forbidden",
  "message": "No puedes modificar productos ajenos",
  "path": "/api/products/1"
}
```

## **5.3. Usuario ADMIN modifica cualquier producto (PERMITIDO)**

```http
# ADMIN puede modificar productos de cualquier usuario
PUT http://localhost:8080/api/products/1
Authorization: Bearer <token-ADMIN>
Body:
{
  "name": "Laptop Moderada por Admin",
  "price": 1100
}

→ 200 OK
{
  "id": 1,
  "name": "Laptop Moderada por Admin",
  "price": 1100,
  "owner": {
    "id": 1,
    "name": "Usuario A"
  }
}
```

## **5.4. Usuario MODERATOR elimina producto ajeno (PERMITIDO)**

```http
# MODERATOR puede eliminar productos de cualquier usuario
DELETE http://localhost:8080/api/products/2
Authorization: Bearer <token-MODERATOR>

→ 204 No Content
```

## **5.5. Usuario USER elimina producto ajeno (DENEGADO)**

```http
# Usuario C intenta eliminar Producto #3 (dueño es Usuario D)
DELETE http://localhost:8080/api/products/3
Authorization: Bearer <token-Usuario-C>

→ 403 Forbidden
{
  "status": 403,
  "error": "Forbidden",
  "message": "No puedes modificar productos ajenos",
  "path": "/api/products/3"
}
```


# **6. Tabla Comparativa de Validaciones**

| Nivel | ¿Qué valida? | ¿Dónde se valida? | Excepción si falla | Código HTTP |
|-------|--------------|-------------------|-------------------|-------------|
| **Autenticación** | ¿Tiene token válido? | JwtAuthenticationFilter | AuthenticationException | 401 Unauthorized |
| **Autorización por Rol** | ¿Tiene el rol necesario? | @PreAuthorize / SecurityConfig | AuthorizationDeniedException | 403 Forbidden |
| **Ownership** | ¿Es dueño del recurso? | Servicio (validateOwnership) | AccessDeniedException | 403 Forbidden |

## **6.1. Ejemplos Prácticos**

### **Caso 1: Usuario sin token**
```
GET /api/products/paginated
Sin header Authorization
→ 401 Unauthorized (Autenticación)
```

### **Caso 2: Usuario USER intenta acceder a endpoint ADMIN**
```
GET /api/products
Authorization: Bearer <token-ROLE_USER>
→ 403 Forbidden (Autorización por Rol)
```

### **Caso 3: Usuario USER intenta modificar producto ajeno**
```
PUT /api/products/1
Authorization: Bearer <token-Usuario-B>
Producto #1 pertenece a Usuario A
→ 403 Forbidden (Ownership)
```

### **Caso 4: ADMIN modifica producto ajeno**
```
PUT /api/products/1
Authorization: Bearer <token-ADMIN>
Producto #1 pertenece a Usuario A
→ 200 OK (ADMIN tiene permiso global)
```


# **7. Mejores Prácticas**

## **7.1. Pasar Usuario desde Controlador al Servicio**

**Correcto** (Enfoque recomendado):
```java
// Controlador
@PutMapping("/{id}")
public ResponseEntity<ProductResponseDto> update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateProductDto dto,
        @AuthenticationPrincipal UserDetailsImpl currentUser) {  // ← Extraer aquí
    
    ProductResponseDto updated = productService.update(id, dto, currentUser);
    return ResponseEntity.ok(updated);
}

// Servicio
@Service
public class ProductService {
    @Transactional
    public ProductResponseDto update(Long id, UpdateProductDto dto, UserDetailsImpl currentUser) {
        ProductEntity product = findProductOrThrow(id);
        validateOwnership(product, currentUser);  // ← Pasar usuario
        // ... actualizar
    }
}
```

**Alternativa** (Usando SecurityContextHolder en el servicio):
```java
// Servicio
@Service
public class ProductService {
    @Transactional
    public ProductResponseDto update(Long id, UpdateProductDto dto) {
        ProductEntity product = findProductOrThrow(id);
        UserDetailsImpl currentUser = getCurrentUser();  // ← Obtener del contexto
        validateOwnership(product, currentUser);
        // ... actualizar
    }
}
```

**Ventajas del enfoque recomendado**:
- Más testeable (no usa estado global)
- Más explícito (se ve qué métodos necesitan el usuario)
- Menos acoplado (no depende de Spring Security internamente)
```

**Incorrecto** :
```java
@RestController
public class ProductController {
    @PutMapping("/{id}")
    @PreAuthorize("@productService.isOwner(#id, authentication.principal.id)")
    public ResponseEntity<ProductResponseDto> update(@PathVariable Long id) {
        // Lógica compleja en @PreAuthorize
    }
}
```

**Razones**:
- Lógica de negocio debe estar en el servicio
- Más fácil de testear
- Reutilizable desde otros lugares
- Expresión SpEL puede ser compleja y difícil de mantener

## **7.2. Roles Privilegiados pueden Saltarse Ownership**

```java
private void validateOwnership(ProductEntity product, UserDetailsImpl currentUser) {

    // ADMIN y MODERATOR tienen permiso global
    if (hasAnyRole(currentUser, "ROLE_ADMIN", "ROLE_MODERATOR")) {
        return;  // ← Saltarse validación
    }

    // Resto de usuarios solo sus recursos
    if (!product.getOwner().getId().equals(currentUser.getId())) {
        throw new AccessDeniedException("No puedes modificar productos ajenos");
    }
}
```

## **7.3. Mensajes de Error Claros**

```java
// Específico
throw new AccessDeniedException("No puedes modificar productos ajenos");

// Genérico 
throw new AccessDeniedException("Access denied");
```

## **6.4. No Exponer Información Sensible**

**Correcto**:
```java
throw new AccessDeniedException("No puedes modificar productos ajenos");
```

**Incorrecto** :
```java
throw new AccessDeniedException(
    "No puedes modificar el producto #1 porque pertenece al usuario juan@example.com"
);
// ← Expone información de otros usuarios
```


# **8. Actividad Práctica**

**Objetivo**: Implementar validación de ownership en tu API.

**Pasos**:

1. **Modificar el Controlador** para extraer el usuario del JWT:
   ```java
   @PutMapping("/{id}")
   public ResponseEntity<ProductResponseDto> update(
           @PathVariable Long id,
           @Valid @RequestBody UpdateProductDto dto,
           @AuthenticationPrincipal UserDetailsImpl currentUser) {
       // ...
   }
   ```

2. **Actualizar métodos del Servicio** para recibir el usuario:
   - `update(Long id, UpdateProductDto dto, UserDetailsImpl currentUser)`
   - `delete(Long id, UserDetailsImpl currentUser)`

3. **Agregar método de validación** en ProductService:
   - `validateOwnership(ProductEntity product, UserDetailsImpl currentUser)`
   - `hasAnyRole(UserDetailsImpl user, String... roles)`

3. **Verificar que tienes el manejador** en GlobalExceptionHandler:
   ```java
   @ExceptionHandler(AccessDeniedException.class)
   public ResponseEntity<ErrorResponse> handleAccessDeniedException(...)
   ```

4. **Crear usuarios de prueba** con diferentes roles:
   ```sql
   -- Usuario A con ROLE_USER
   -- Usuario B con ROLE_USER
   -- Usuario C con ROLE_ADMIN
   ```

5. **Crear productos** con diferentes propietarios:
   ```sql
   -- Producto #1 → owner_id = Usuario A
   -- Producto #2 → owner_id = Usuario B
   ```

6. **Probar con Postman**:

   a. Usuario A actualiza su propio producto (#1):
   ```http
   PUT /api/products/1
   Authorization: Bearer <token-Usuario-A>
   → 200 OK
   ```

   b. Usuario B intenta actualizar producto de A (#1):
   ```http
   PUT /api/products/1
   Authorization: Bearer <token-Usuario-B>
   → 403 Forbidden con mensaje "No puedes modificar productos ajenos"
   ```

   c. Usuario ADMIN actualiza producto de A (#1):
   ```http
   PUT /api/products/1
   Authorization: Bearer <token-ADMIN>
   → 200 OK
   ```

   d. Usuario B elimina su propio producto (#2):
   ```http
   DELETE /api/products/2
   Authorization: Bearer <token-Usuario-B>
   → 204 No Content
   ```

   e. Usuario A intenta eliminar producto de B (ya eliminado):
   ```http
   DELETE /api/products/2
   Authorization: Bearer <token-Usuario-A>
   → 404 Not Found (porque ya fue eliminado)
   ```

**Resultado esperado**:
- Usuarios pueden modificar/eliminar sus propios recursos
- Usuarios NO pueden modificar/eliminar recursos ajenos (403)
- ADMIN y MODERATOR pueden modificar/eliminar cualquier recurso
- Mensajes de error claros y específicos


# **9. Conclusiones**

**@AuthenticationPrincipal** en el controlador extrae el usuario del JWT automáticamente

**Validación de ownership** se hace en el servicio recibiendo el usuario como parámetro

**Enfoque testeable**: No depende de `SecurityContextHolder` estático

**AccessDeniedException** se lanza automáticamente cuando no eres dueño

**GlobalExceptionHandler** ya maneja la excepción (si tienes el manejador de la Práctica 12)

**ADMIN y MODERATOR** pueden saltarse validación de ownership

**Mensajes claros** ayudan al frontend a mostrar errores específicos

**3 capas de seguridad**: Autenticación → Autorización por rol → Ownership

**Próximos pasos**:
- Implementar ownership en otros recursos (comentarios, órdenes, etc.)
- Agregar auditoría (quién modificó qué y cuándo)
- Implementar soft delete para recursos sensibles
- Agregar logs de intentos de acceso no autorizado