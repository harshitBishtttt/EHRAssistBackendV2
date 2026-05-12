# Coding Conventions

## Principles
- Production-grade, scalable, reusable — no temporary fixes
- Discuss approach before implementing new features
- Modular design (future plugin architecture possible)
- FHIR R4 compliance for all clinical responses

## Null/Empty Handling
```java
// ALWAYS use:
if (!ObjectUtils.isEmpty(value)) { ... }
if (!CollectionUtils.isEmpty(list)) { ... }
// NEVER use raw != null
```

## Collections
```java
// ALWAYS streams:
List<Resource> resources = entities.stream()
    .map(mapper::toFhirResource)
    .map(Resource.class::cast)
    .toList();
// AtomicInteger for sequence numbering in streams
```

## Pagination
All list APIs use `Page<T>` + `Pageable`, default size 10.
```java
@PageableDefault(page = 0, size = 10) Pageable pageable
```

## N+1 Prevention (use-case based)
- Heavy APIs → `JOIN FETCH` (JPQL) or native JOIN + interface projection
- Simple lookups → N+1 acceptable
- When unclear, discuss first

## Entity Patterns
```java
// Clinical (extends BaseEntity)
@SuperBuilder @EqualsAndHashCode(callSuper = true)

// Lightweight
@Builder with standalone @Id @GeneratedValue(UUID)

// Relationships
@ManyToOne(LAZY) + @ToString.Exclude + @EqualsAndHashCode.Exclude
@OneToMany(mappedBy, cascade=ALL, orphanRemoval=true)
```

## Service Layer
- Interface + Impl, `@Transactional` on class, `readOnly=true` on reads
- Validate referenced entities before saving
- `ResourceNotFoundException` → 404

## Repository
- `JpaRepository<Entity, UUID>` + `JpaSpecificationExecutor` where needed
- Interface projections for native queries, always with `countQuery`

## Naming
| Type | Pattern |
|------|---------|
| Entity | `{Resource}Entity` |
| Repository | `{Resource}Repository` |
| Service | `{Resource}Service` / `{Resource}ServiceImpl` |
| Mapper | `{Resource}Mapper` |
| Request DTO | `{Action}{Resource}Request` |
| Response DTO | `{Resource}Response` |
| Projection | `{Purpose}Projection` |
