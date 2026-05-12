# Coding Conventions

## Mandatory
- `ObjectUtils.isEmpty()` / `CollectionUtils.isEmpty()` for all null/empty checks — never raw `!= null`
- Java 8+ streams for collection transforms — never for-loops. `AtomicInteger` for sequencing.
- `Page<T>` + `Pageable` on ALL list APIs. Default size 10.
- Group endpoints in existing controllers — no extra controller classes.
- `@PreAuthorize` on all endpoints with correct roles.
- Interface + Impl for services. `@Transactional` on class, `readOnly = true` on reads.
- Discuss approach before implementing new features.

## Entity
- Clinical entities extend `BaseEntity` (id, version, createdAt, updatedAt) with `@SuperBuilder`, `@EqualsAndHashCode(callSuper=true)`
- Lightweight entities: standalone `@Id @GeneratedValue(UUID)` with `@Builder`
- `@ManyToOne(LAZY)` + `@ToString.Exclude` + `@EqualsAndHashCode.Exclude`
- `@OneToMany(mappedBy, cascade=ALL, orphanRemoval=true)` for children

## Repository
- `JpaRepository<Entity, UUID>` + `JpaSpecificationExecutor` where needed
- Derived methods for simple queries, `@Query` for complex joins
- Interface projections for native read-only queries
- Always `countQuery` for native paginated queries

## N+1 Prevention (use-case based)
- Heavy list APIs with nested associations → `JOIN FETCH` (JPQL) or native `INNER JOIN` + projection
- Simple lookups / single GET by ID → N+1 acceptable
- When unclear, discuss before implementing

## FHIR
- Single resource → `FhirResponseHelper.toResponse(Resource)`
- Lists → `BundleBuilder.searchSetWithPagination()` → FhirResponseHelper
- Custom fields → FHIR `Extension`
- Errors → `OperationOutcome` (404/400/403/401/500)

## Naming
Entity: `{Resource}Entity` | Repo: `{Resource}Repository` | Service: `{Resource}Service`/`Impl` | Mapper: `{Resource}Mapper` | Request DTO: `{Action}{Resource}Request` | Response DTO: `{Resource}Response` | Projection: `{Purpose}Projection`

## Design
- Production-grade, scalable, reusable — no temp fixes
- Modular (future plugin architecture) but don't over-engineer
- Validate referenced entities before saving
- `ResourceNotFoundException` → 404, `ResponseStatusException` → 400/403/409
