package africa.flot.infrastructure.resource.rest;

import africa.flot.application.auth.AdminAuthCommand;
import africa.flot.application.auth.SubscriberAuthCommand;
import africa.flot.application.dto.command.ChangePasswordCommand;
import africa.flot.infrastructure.security.AuthService;
import africa.flot.infrastructure.util.ApiResponseBuilder;
import io.minio.MinioClient;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.time.Duration;

@Path("/auth")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "API for managing user authentication")
public class AuthResource {

    @Inject
    AuthService authService;

    @Inject
    MinioClient minioClient;

    @Inject
    io.vertx.mutiny.pgclient.PgPool client;

    private static final Logger AUDIT_LOG = Logger.getLogger("AUDIT");
    private static final Logger ERROR_LOG = Logger.getLogger("ERROR");
    private static final Logger BUSINESS_LOG = Logger.getLogger("BUSINESS");

    @ConfigProperty(name = "jwt.duration", defaultValue = "PT1H")
    Duration jwtDuration;

    @POST
    @PermitAll
    @Path("/lead/login")
    @WithSession
    @Operation(summary = "Authenticate a subscriber", description = "Allows a subscriber to log in using a phone number and password.")
    @APIResponse(responseCode = "200", description = "Authentication successful", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(responseCode = "401", description = "Invalid credentials", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(responseCode = "400", description = "Invalid input", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Uni<Response> loginSubscriber(SubscriberAuthCommand command) {
        if (command == null || command.phone() == null || command.password() == null) {
            ERROR_LOG.warn("loginSubscriber: Invalid command received, phone or password is null");
            return Uni.createFrom().item(ApiResponseBuilder.failure("Invalid input: phone and password must not be null", Response.Status.BAD_REQUEST));
        }
        BUSINESS_LOG.info("loginSubscriber: Attempting login for subscriber with phone: " + command.phone());
        return authService.authenticateSubscriber(command.phone(), command.password())
                .onItem().transformToUni(authenticated -> {
                    if (authenticated) {
                        BUSINESS_LOG.info("loginSubscriber: Authentication successful for phone: " + command.phone());
                        return authService.generateSubscriberJWT(command.phone())
                                .onItem().transform(jwtData -> {
                                    if (jwtData != null) {
                                        AUDIT_LOG.info("loginSubscriber: JWT generated for subscriber with phone: " + command.phone());
                                        return ApiResponseBuilder.success(jwtData);
                                    } else {
                                        ERROR_LOG.warn("loginSubscriber: JWT generation failed for phone: " + command.phone());
                                        return ApiResponseBuilder.failure("Error generating JWT", Response.Status.INTERNAL_SERVER_ERROR);
                                    }
                                });
                    } else {
                        ERROR_LOG.warn("loginSubscriber: Authentication failed for phone: " + command.phone());
                        return Uni.createFrom().item(ApiResponseBuilder.failure("Invalid credentials", Response.Status.UNAUTHORIZED));
                    }
                });
    }


    @POST
    @PermitAll
    @Path("/admin/login")
    @WithSession
    @Operation(summary = "Authenticate an admin", description = "Allows an admin to log in using an email, API key, and session ID.")
    @APIResponse(responseCode = "200", description = "Authentication successful", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(responseCode = "401", description = "Invalid credentials or session", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(responseCode = "400", description = "Invalid input", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Uni<Response> loginAdmin(AdminAuthCommand command) {
        if (command == null || command.email() == null || command.apiKey() == null || command.sessionId() == null) {
            ERROR_LOG.warn("loginAdmin: email, API Key, or session ID is null");
            return Uni.createFrom().item(ApiResponseBuilder.failure("Invalid input: email, API Key, and session ID must not be null", Response.Status.BAD_REQUEST));
        }

        BUSINESS_LOG.info("loginAdmin: Attempting admin login for email: " + command.email());
        return authService.authenticateAdminWithApiKeyAndSession(command.email(), command.apiKey(), command.sessionId()).onItem().transform(jwtData -> {
            if (jwtData != null) {
                AUDIT_LOG.info("loginAdmin: JWT generated for admin with email: " + command.email());
                return ApiResponseBuilder.success(jwtData);
            } else {
                ERROR_LOG.warn("loginAdmin: Authentication failed for email: " + command.email());
                return ApiResponseBuilder.failure("Invalid credentials or session", Response.Status.UNAUTHORIZED);
            }
        });
    }


    @GET
    @Path("/me")
    @RolesAllowed({"SUBSCRIBER", "ADMIN"})
    @Operation(summary = "Retrieve authenticated user info", description = "Returns the information of the currently authenticated user, including their photo.")
    @APIResponse(responseCode = "200", description = "User info retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(responseCode = "401", description = "User not authenticated", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Uni<Response> me(@Context SecurityContext securityContext) {
        if (securityContext.getUserPrincipal() == null) {
            ERROR_LOG.warn("me: No authenticated user found");
            return Uni.createFrom().item(ApiResponseBuilder.failure("User not authenticated", Response.Status.UNAUTHORIZED));
        }

        String usernameOrEmail = securityContext.getUserPrincipal().getName();
        String role = securityContext.isUserInRole("SUBSCRIBER") ? "SUBSCRIBER" : securityContext.isUserInRole("ADMIN") ? "ADMIN" : null;

        if (role == null) {
            ERROR_LOG.warn("me: User role unknown");
            return Uni.createFrom().item(ApiResponseBuilder.failure("Unknown role", Response.Status.FORBIDDEN));
        }

        return authService.getUserInfo(usernameOrEmail, role).onItem().transform(userInfo -> {
            if (userInfo == null) {
                ERROR_LOG.warn("me: User not found for identifier: " + usernameOrEmail);
                return ApiResponseBuilder.failure("User not found", Response.Status.NOT_FOUND);
            }
            AUDIT_LOG.info("me: User info retrieved successfully for: " + usernameOrEmail);
            return ApiResponseBuilder.success(userInfo);
        }).onFailure().recoverWithItem(throwable -> {
            ERROR_LOG.error("me: Error retrieving user info: " + throwable.getMessage());
            return ApiResponseBuilder.failure("Error retrieving user info", Response.Status.INTERNAL_SERVER_ERROR);
        });
    }

    @POST
    @Path("/logout")
    @RolesAllowed({"SUBSCRIBER", "ADMIN"})
    @Operation(summary = "Logout a user", description = "Invalidates the authentication token of the current user.")
    @APIResponse(responseCode = "200", description = "Logout successful", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(responseCode = "400", description = "Token not found", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(responseCode = "401", description = "User not authenticated", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Uni<Response> logout(@Context SecurityContext securityContext, @Context ContainerRequestContext requestContext) {
        if (securityContext.getUserPrincipal() == null) {
            ERROR_LOG.warn("logout: No authenticated user found");
            return Uni.createFrom().item(ApiResponseBuilder.failure("User not authenticated", Response.Status.UNAUTHORIZED));
        }

        String token = extractToken(requestContext);
        if (token == null) {
            ERROR_LOG.warn("logout: No token found");
            return Uni.createFrom().item(ApiResponseBuilder.failure("Token not found", Response.Status.BAD_REQUEST));
        }

        BUSINESS_LOG.info("logout: Invalidating token for current user");
        return authService.invalidateToken(token).onItem().transform(isInvalidated -> {
            if (isInvalidated) {
                AUDIT_LOG.info("logout: Token invalidated successfully");
                return ApiResponseBuilder.success("Logout successful");
            } else {
                ERROR_LOG.warn("logout: Token invalidation failed");
                return ApiResponseBuilder.failure("Logout failed", Response.Status.INTERNAL_SERVER_ERROR);
            }
        });
    }

    @POST
    @Path("/change-password")
    @RolesAllowed({"SUBSCRIBER", "ADMIN"})
    @Operation(summary = "Change a user's password", description = "Allows a user to change their password.")
    @APIResponse(responseCode = "200", description = "Password changed successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(responseCode = "400", description = "Invalid input", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(responseCode = "401", description = "User not authenticated", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Uni<Response> changePassword(@Context SecurityContext securityContext, ChangePasswordCommand command) {
        if (command == null || command.getOldPassword() == null || command.getNewPassword() == null) {
            ERROR_LOG.warn("changePassword: Invalid input provided for password change");
            return Uni.createFrom().item(ApiResponseBuilder.failure("Invalid input", Response.Status.BAD_REQUEST));
        }

        String username = securityContext.getUserPrincipal().getName();
        BUSINESS_LOG.info("changePassword: Attempting to change password for user: " + username);

        return authService.changePassword(username, command.getOldPassword(), command.getNewPassword()).onItem().transform(response -> {
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                AUDIT_LOG.info("changePassword: Password changed successfully for " + username);
            } else {
                ERROR_LOG.warn("changePassword: Failed to change password for " + username);
            }
            return response;
        });
    }

    private String extractToken(ContainerRequestContext requestContext) {
        String authHeader = requestContext.getHeaderString("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
