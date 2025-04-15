package africa.flot.infrastructure.security;

import africa.flot.infrastructure.repository.TokenBlacklistRepository;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.annotation.Priority;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import java.lang.reflect.Method;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthenticationFilter {

    private static final Logger LOG = Logger.getLogger(JwtAuthenticationFilter.class);

    @Inject
    TokenBlacklistRepository tokenBlacklistRepository;

    @Inject
    JWTParser parser;

    @Context
    ResourceInfo resourceInfo;

    @ServerRequestFilter
    public Uni<Response> filter(ContainerRequestContext requestContext) {
        if (isPermitAll() || isPasswordChangeEndpoint(requestContext)) {
            return Uni.createFrom().nullItem();
        }

        String token = extractToken(requestContext);
        if (token == null) {
            return abortRequest(
                    "Token manquant",
                    "MISSING_TOKEN",
                    Response.Status.UNAUTHORIZED
            );
        }

        return tokenBlacklistRepository.isTokenBlacklisted(token)
                .onFailure().invoke(e -> LOG.error("Erreur lors de la vérification du token", e))
                .onItem().transformToUni(isBlacklisted -> {
                    if (isBlacklisted) {
                        return abortRequest(
                                "Token invalidé",
                                "INVALID_TOKEN",
                                Response.Status.UNAUTHORIZED
                        );
                    }

                    if (isSubscriberToken(token)) {
                        boolean requirePasswordChange = Boolean.parseBoolean(
                                extractClaimFromToken(token, "requirePasswordChange")
                        );

                        if (requirePasswordChange) {
                            return abortRequest(
                                    "Le mot de passe doit être modifié",
                                    "PASSWORD_CHANGE_REQUIRED",
                                    Response.Status.FORBIDDEN
                            );
                        }
                    }
                    return Uni.createFrom().nullItem();
                })
                .onFailure().recoverWithItem(e -> {
                    LOG.error("Erreur interne lors de l'authentification", e);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(new JsonObject()
                                    .put("status", "failure")
                                    .put("message", "Erreur interne")
                                    .put("code", "INTERNAL_ERROR")
                                    .encode())
                            .header("Content-Type", "application/json")
                            .build();
                });
    }

    private boolean isSubscriberToken(String token) {
        try {
            JsonWebToken jwt = parser.parse(token);
            return jwt.getGroups().contains("SUBSCRIBER");
        } catch (ParseException e) {
            LOG.error("Erreur lors de la vérification du type de token", e);
            return false;
        }
    }

    private String extractClaimFromToken(String token, String claimName) {
        try {
            JsonWebToken jwt = parser.parse(token);
            Object claim = jwt.getClaim(claimName);

            if (claim instanceof String) {
                return (String) claim;
            } else if (claim instanceof jakarta.json.JsonValue) {
                return claim.toString();
            } else if (claim != null) {
                return claim.toString();
            }
            return null;
        } catch (ParseException e) {
            LOG.error("Erreur lors de l'extraction du claim " + claimName, e);
            return null;
        }
    }

    private boolean isPasswordChangeEndpoint(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        String method = requestContext.getMethod();
        return "/auth/change-password".equals(path) && "POST".equalsIgnoreCase(method);
    }

    private boolean isPermitAll() {
        Class<?> resourceClass = resourceInfo.getResourceClass();
        Method resourceMethod = resourceInfo.getResourceMethod();
        return resourceClass.isAnnotationPresent(PermitAll.class) ||
                resourceMethod.isAnnotationPresent(PermitAll.class);
    }

    private String extractToken(ContainerRequestContext requestContext) {
        String authHeader = requestContext.getHeaderString("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private Uni<Response> abortRequest(String message, String errorCode, Response.Status status) {
        JsonObject errorResponse = new JsonObject()
                .put("status", "failure")
                .put("message", message)
                .put("code", errorCode);

        return Uni.createFrom().item(
                Response.status(status)
                        .entity(errorResponse.encode())
                        .header("Content-Type", "application/json")
                        .build()
        );
    }
}
