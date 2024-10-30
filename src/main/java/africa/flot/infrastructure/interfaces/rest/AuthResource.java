package africa.flot.infrastructure.interfaces.rest;

import africa.flot.application.auth.AdminAuthCommand;
import africa.flot.application.auth.SubscriberAuthCommand;
import africa.flot.application.dto.command.ChangePasswordCommand;
import africa.flot.infrastructure.security.AuthService;
import africa.flot.infrastructure.util.ApiResponseBuilder;
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
import org.jboss.logging.Logger;

@Path("/auth")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    private static final Logger LOG = Logger.getLogger(ApiResponseBuilder.class);


    @POST
    @PermitAll
    @Path("/subscriber/login")
    @WithSession
    public Uni<Response> loginSubscriber(SubscriberAuthCommand command) {
        if (command == null || command.phone() == null || command.password() == null) {
            LOG.warn("loginSubscriber: Invalid command received, phone or password is null");
            return Uni.createFrom().item(ApiResponseBuilder.failure("Invalid input: phone and password must not be null", Response.Status.BAD_REQUEST));
        }
        LOG.info("loginSubscriber: Attempting login for subscriber with phone: " + command.phone());
        return authService.authenticateSubscriber(command.phone(), command.password())
                .onItem().transformToUni(authenticated -> {
                    if (authenticated) {
                        LOG.info("loginSubscriber: Authentication successful for phone: " + command.phone());
                        return authService.generateSubscriberJWT(command.phone())
                                .onItem().transform(jwtData -> {
                                    LOG.info("loginSubscriber: JWT generated for subscriber with phone: " + command.phone());
                                    return ApiResponseBuilder.success(jwtData);
                                });
                    } else {
                        LOG.warn("loginSubscriber: Authentication failed for phone: " + command.phone());
                        return Uni.createFrom().item(ApiResponseBuilder.failure("Invalid credentials", Response.Status.UNAUTHORIZED));
                    }
                });
    }

    @POST
    @PermitAll
    @Path("/admin/login")
    @WithSession
    public Uni<Response> loginAdmin(AdminAuthCommand command) {
        if (command == null || command.email() == null || command.password() == null) {
            LOG.warn("loginAdmin: Invalid command received, email or password is null");
            return Uni.createFrom().item(ApiResponseBuilder.failure("Invalid input: email and password must not be null", Response.Status.BAD_REQUEST));
        }
        LOG.info("loginAdmin: Attempting login for admin with email: " + command.email());
        return authService.authenticateAdmin(command.email(), command.password())
                .onItem().transformToUni(authenticated -> {
                    if (authenticated) {
                        LOG.info("loginAdmin: Authentication successful for email: " + command.email());
                        return authService.generateAdminJWT(command.email())
                                .onItem().transform(jwtData -> {
                                    LOG.info("loginAdmin: JWT generated for admin with email: " + command.email());
                                    return ApiResponseBuilder.success(jwtData);
                                });
                    } else {
                        LOG.warn("loginAdmin: Authentication failed for email: " + command.email());
                        return Uni.createFrom().item(ApiResponseBuilder.failure("Invalid credentials", Response.Status.UNAUTHORIZED));
                    }
                });
    }

    @GET
    @Path("/me")
    @RolesAllowed({"SUBSCRIBER", "ADMIN"})
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> me(@Context SecurityContext securityContext) {
        if (securityContext.getUserPrincipal() == null) {
            LOG.warn("me: Aucun utilisateur authentifié trouvé");
            return Uni.createFrom().item(ApiResponseBuilder.failure("Utilisateur non authentifié", Response.Status.UNAUTHORIZED));
        }

        String usernameOrEmail = securityContext.getUserPrincipal().getName();
        LOG.info("me: Récupération des informations pour l'utilisateur : " + usernameOrEmail);

        String role = securityContext.isUserInRole("SUBSCRIBER") ? "SUBSCRIBER" :
                securityContext.isUserInRole("ADMIN") ? "ADMIN" : null;

        if (role == null) {
            LOG.warn("me: Rôle de l'utilisateur inconnu");
            return Uni.createFrom().item(ApiResponseBuilder.failure("Rôle inconnu", Response.Status.FORBIDDEN));
        }

        return authService.getAuthenticatedUser(usernameOrEmail, role)
                .onItem().transform(user -> {
                    if (user != null) {
                        return ApiResponseBuilder.success(user);
                    } else {
                        LOG.warn("me: Utilisateur non trouvé pour le nom d'utilisateur ou email : " + usernameOrEmail);
                        return ApiResponseBuilder.failure("Utilisateur non trouvé", Response.Status.NOT_FOUND);
                    }
                });
    }

    @POST
    @Path("/logout")
    @RolesAllowed({"SUBSCRIBER", "ADMIN"})
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> logout(@Context SecurityContext securityContext, @Context ContainerRequestContext requestContext) {
        if (securityContext.getUserPrincipal() == null) {
            LOG.warn("logout: Aucun utilisateur authentifié trouvé");
            return Uni.createFrom().item(ApiResponseBuilder.failure("Utilisateur non authentifié", Response.Status.UNAUTHORIZED));
        }

        String token = extractToken(requestContext);
        if (token == null) {
            LOG.warn("logout: Aucun token trouvé");
            return Uni.createFrom().item(ApiResponseBuilder.failure("Token non trouvé", Response.Status.BAD_REQUEST));
        }

        return authService.invalidateToken(token)
                .onItem().transform(isInvalidated -> {
                    if (isInvalidated) {
                        LOG.info("logout: Token invalidé avec succès");
                        return ApiResponseBuilder.success("Déconnexion réussie");
                    } else {
                        LOG.warn("logout: Échec de l'invalidation du token");
                        return ApiResponseBuilder.failure("Échec de la déconnexion", Response.Status.INTERNAL_SERVER_ERROR);
                    }
                });
    }

    @POST
    @Path("/change-password")
    @RolesAllowed({"SUBSCRIBER", "ADMIN"})
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> changePassword(@Context SecurityContext securityContext, ChangePasswordCommand command) {
        if (command == null || command.getOldPassword() == null || command.getNewPassword() == null) {
            LOG.warn("changePassword: Données invalides fournies pour la modification du mot de passe");
            return Uni.createFrom().item(ApiResponseBuilder.failure("Données invalides", Response.Status.BAD_REQUEST));
        }

        String username = securityContext.getUserPrincipal().getName();
        LOG.info("changePassword: Tentative de modification du mot de passe pour l'utilisateur : " + username);

        return authService.changePassword(username, command.getOldPassword(), command.getNewPassword())
                .onItem().transform(response -> {
                    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                        LOG.info("changePassword: Mot de passe modifié avec succès pour " + username);
                    } else {
                        LOG.warn("changePassword: Échec de la modification du mot de passe pour " + username);
                    }
                    return response;
                });
    }

    // Méthode pour extraire le token depuis ContainerRequestContext
    private String extractToken(ContainerRequestContext requestContext) {
        String authHeader = requestContext.getHeaderString("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}