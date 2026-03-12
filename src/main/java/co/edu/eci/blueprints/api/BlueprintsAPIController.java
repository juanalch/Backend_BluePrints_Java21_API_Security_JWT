package co.edu.eci.blueprints.api;

import co.edu.eci.blueprints.model.Blueprint;
import co.edu.eci.blueprints.model.Point;
import co.edu.eci.blueprints.dto.BlueprintDTO;
import co.edu.eci.blueprints.dto.PointDTO;
import co.edu.eci.blueprints.dto.BlueprintMapper;
import co.edu.eci.blueprints.persistence.BlueprintNotFoundException;
import co.edu.eci.blueprints.persistence.BlueprintPersistenceException;
import co.edu.eci.blueprints.services.BlueprintsServices;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.Set;

/**
 * REST controller for managing blueprint resources.
 * Exposes HTTP endpoints for retrieving, creating, and updating blueprints.
 * Delegates business logic to the BlueprintsServices class.
 */
@RestController
@RequestMapping("/api/v1/blueprints")
@CrossOrigin(origins = "http://localhost:5173")
public class BlueprintsAPIController {

    /**
     * Service layer for blueprint operations.
     */
    private final BlueprintsServices services;

    /**
     * Constructs the controller with the required service dependency.
     * @param services The BlueprintsServices instance
     */
    public BlueprintsAPIController(BlueprintsServices services) { this.services = services; }

    /**
     * Retrieves all blueprints in the system.
     * @return HTTP 200 with the set of all blueprints
     */
    @Operation(summary = "Obtener todos los blueprints")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de blueprints obtenida exitosamente"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "No se encontraron blueprints",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\"code\":404,\"message\":\"No se encontraron blueprints\",\"data\":null}"
                )
            )
        )
    })
    @PreAuthorize("hasAuthority('SCOPE_blueprints.read')")
    @GetMapping
    public ResponseEntity<ApiResponse<Set<BlueprintDTO>>> getAll() {
        Set<Blueprint> data = services.getAllBlueprints();
        Set<BlueprintDTO> dtoSet = data.stream().map(BlueprintMapper::toDTO).collect(java.util.stream.Collectors.toSet());
        return ResponseEntity.ok(new ApiResponse<>(200, "Success", dtoSet)); // 200 OK
    }

    /**
     * Retrieves all blueprints created by a specific author.
     * @param author The author's name
     * @return HTTP 200 with the set of blueprints, or 404 if none found
     */
    @Operation(summary = "Obtener todos los blueprints de un autor")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de blueprints del autor obtenida exitosamente"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "No se encontraron blueprints para el autor",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\"code\":404,\"message\":\"No se encontraron blueprints para el autor\",\"data\":null}"
                )
            )
        )
    })
    @PreAuthorize("hasAuthority('SCOPE_blueprints.read')")
    @GetMapping("/{author}")
    public ResponseEntity<ApiResponse<Set<BlueprintDTO>>> byAuthor(@PathVariable String author) {
        try {
            Set<Blueprint> data = services.getBlueprintsByAuthor(author);
            Set<BlueprintDTO> dtoSet = data.stream().map(BlueprintMapper::toDTO).collect(java.util.stream.Collectors.toSet());
            return ResponseEntity.ok(new ApiResponse<>(200, "Success", dtoSet)); // 200 OK
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(404, e.getMessage(), null)); // 404 Not Found
        }
    }

    /**
     * Retrieves a specific blueprint by author and blueprint name.
     * @param author The author's name
     * @param bpname The blueprint's name
     * @return HTTP 200 with the blueprint, or 404 if not found
     */
    @Operation(summary = "Obtener un blueprint por autor y nombre")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Blueprint obtenido exitosamente"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Blueprint no encontrado",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\"code\":404,\"message\":\"Blueprint no encontrado\",\"data\":null}"
                )
            )
        )
    })
    @PreAuthorize("hasAuthority('SCOPE_blueprints.read')")
    @GetMapping("/{author}/{bpname}")
    public ResponseEntity<ApiResponse<BlueprintDTO>> byAuthorAndName(@PathVariable String author, @PathVariable String bpname) {
        try {
            Blueprint data = services.getBlueprint(author, bpname);
            BlueprintDTO dto = BlueprintMapper.toDTO(data);
            return ResponseEntity.ok(new ApiResponse<>(200, "Success", dto)); // 200 OK
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(404, e.getMessage(), null)); // 404 Not Found
        }
    }

    /**
     * Creates a new blueprint with the provided data.
     * @param req The request body containing author, name, and points
     * @return HTTP 201 if created, or 403 if a blueprint with the same key already exists
     */
    @Operation(summary = "Agregar un nuevo blueprint")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Blueprint creado exitosamente"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Solicitud inválida o datos incorrectos",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\"code\":400,\"message\":\"Solicitud inválida o datos incorrectos\",\"data\":null}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "El blueprint ya existe",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\"code\":409,\"message\":\"El blueprint ya existe\",\"data\":null}"
                )
            )
        )
    })
    @PreAuthorize("hasAuthority('SCOPE_blueprints.write')")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> add(@Valid @RequestBody NewBlueprintRequest req) {
        try {
            // Convertir los PointDTO a Point
            java.util.List<Point> points = req.points().stream().map(BlueprintMapper::toEntity).toList();
            Blueprint bp = new Blueprint(req.author(), req.name(), points);
            services.addNewBlueprint(bp);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(201, "Created", null)); // 201 Created
        } catch (BlueprintPersistenceException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(400, e.getMessage(), null)); // 400 Bad Request
        }
    }

    /**
     * Adds a new point to an existing blueprint.
     * @param author The author's name
     * @param bpname The blueprint's name
     * @param p The point to add
     * @return HTTP 202 if accepted, or 404 if the blueprint is not found
     */
    @Operation(summary = "Agregar un punto a un blueprint existente")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "Punto agregado exitosamente"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Blueprint no encontrado",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\"code\":404,\"message\":\"Blueprint no encontrado\",\"data\":null}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Solicitud inválida o datos incorrectos",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\"code\":400,\"message\":\"Solicitud inválida o datos incorrectos\",\"data\":null}"
                )
            )
        )
    })
    @PreAuthorize("hasAuthority('SCOPE_blueprints.addPoint')")
    @PutMapping("/{author}/{bpname}/points")
    public ResponseEntity<ApiResponse<Void>> addPoint(@PathVariable String author, @PathVariable String bpname,
                                      @RequestBody PointDTO p) {
        try {
            services.addPoint(author, bpname, p.getX(), p.getY());
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(201, "Created", null)); // 201 Created
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(404, e.getMessage(), null)); // 404 Not Found
        }
    }

    /**
     * Updates a blueprint and optionally allows changing its author, name, and points.
     * @param author Current blueprint author
     * @param bpname Current blueprint name
     * @param req Updated blueprint data
     * @return HTTP 200 with the updated blueprint, or an error status if the update fails
     */
    @Operation(summary = "Actualizar un blueprint existente")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Blueprint actualizado exitosamente"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Solicitud inválida o datos incorrectos",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\"code\":400,\"message\":\"Solicitud inválida o datos incorrectos\",\"data\":null}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Blueprint no encontrado",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\"code\":404,\"message\":\"Blueprint no encontrado\",\"data\":null}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "El blueprint ya existe",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\"code\":409,\"message\":\"El blueprint ya existe\",\"data\":null}"
                )
            )
        )
    })
    @PreAuthorize("hasAuthority('SCOPE_blueprints.write')")
    @PutMapping("/{author}/{bpname}")
    public ResponseEntity<ApiResponse<BlueprintDTO>> update(@PathVariable String author, @PathVariable String bpname,
                                                            @Valid @RequestBody UpdateBlueprintRequest req) {
        if (req.points() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(400, "Solicitud inválida o datos incorrectos", null));
        }

        try {
            java.util.List<Point> points = req.points().stream().map(BlueprintMapper::toEntity).toList();
            Blueprint updatedBlueprint = new Blueprint(req.author(), req.name(), points);
            Blueprint updated = services.updateBlueprint(author, bpname, updatedBlueprint);
            return ResponseEntity.ok(new ApiResponse<>(200, "Success", BlueprintMapper.toDTO(updated)));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(404, e.getMessage(), null));
        } catch (BlueprintPersistenceException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>(409, e.getMessage(), null));
        }
    }

    /**
     * Deletes a blueprint by author and name.
     * @param author The blueprint author
     * @param bpname The blueprint name
     * @return HTTP 200 if deleted, or 404 if not found
     */
    @Operation(summary = "Eliminar un blueprint existente")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Blueprint eliminado exitosamente"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Blueprint no encontrado",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\"code\":404,\"message\":\"Blueprint no encontrado\",\"data\":null}"
                )
            )
        )
    })
    @PreAuthorize("hasAuthority('SCOPE_blueprints.write')")
    @DeleteMapping("/{author}/{bpname}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String author, @PathVariable String bpname) {
        try {
            services.deleteBlueprint(author, bpname);
            return ResponseEntity.ok(new ApiResponse<>(200, "Deleted", null));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(404, e.getMessage(), null));
        }
    }

    /**
     * Request body model for creating a new blueprint.
     * Encapsulates and validates the required fields: author, name, and points.
     */
        public record NewBlueprintRequest(
            @NotBlank String author,
            @NotBlank String name,
            @Valid java.util.List<PointDTO> points
        ) { }

        public record UpdateBlueprintRequest(
            @NotBlank String author,
            @NotBlank String name,
            @Valid java.util.List<PointDTO> points
        ) { }
}
