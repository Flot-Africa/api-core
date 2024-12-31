package africa.flot.infrastructure.security;

import africa.flot.infrastructure.repository.TokenBlacklistRepository;
import africa.flot.infrastructure.util.ApiResponseBuilder;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.mutiny.Uni;
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

    private static final Logger errorf_LOG = Logger.getLogger("errorf");

    @Inject
    TokenBlacklistRepository tokenBlacklistRepository;

    @Inject
    JWTParser parser;

    @Context
    ResourceInfo resourceInfo;

    @ServerRequestFilter
    public Uni<Response> filter(ContainerRequestContext requestContext) {
        if (isPermitAll() || isPasswordChangeEndpoint(requestContext)) {
            return null; // Continuer la chaîne de filtres
        }

        String token = extractToken(requestContext);
        if (token == null) {
            return abortRequest("Token manquant");
        }

        return tokenBlacklistRepository.isTokenBlacklisted(token)
                .onFailure().invoke(e -> errorf_LOG.error("Erreur lors de la vérification du token", e))
                .onItem().transformToUni(isBlacklisted -> {
                    if (isBlacklisted) {
                        return abortRequest("Token invalidé");
                    }

                    if (isSubscriberToken(token)) {
                        String username = extractClaimFromToken(token, "username");
                        boolean requirePasswordChange = Boolean.parseBoolean(extractClaimFromToken(token, "requirePasswordChange"));

                        if (requirePasswordChange) {
                            return abortRequest("Le mot de passe doit être modifié");
                        }
                    }
                    return Uni.createFrom().nullItem(); // Continuer sans erreur
                })
                .onFailure().recoverWithItem(e ->
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity(ApiResponseBuilder.failure("Erreur interne", Response.Status.INTERNAL_SERVER_ERROR))
                                .build()
                );
    }


    private boolean isSubscriberToken(String token) {
        try {
            JsonWebToken jwt = parser.parse(token);
            return jwt.getGroups().contains("SUBSCRIBER");
        } catch (ParseException e) {
            errorf_LOG.error("Erreur lors de la vérification du type de token", e);
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
                jakarta.json.JsonValue jsonValue = (jakarta.json.JsonValue) claim;
                return jsonValue.toString();
            } else if (claim != null) {
                // Autre type de claim : convertir en chaîne JSON.
                return claim.toString();
            }
            return null;
        } catch (ParseException e) {
            errorf_LOG.error("Erreur lors de l'extraction du claim " + claimName + " du token", e);
            return null;
        }
    }


    private boolean isPasswordChangeEndpoint(ContainerRequestContext requestContext) {
        // Vérifiez si l'URI correspond à /change-password et que la méthode est POST
        String path = requestContext.getUriInfo().getPath();
        String method = requestContext.getMethod();
        errorf_LOG.error("path " + path + " method"+ method);
        return "/auth/change-password".equals(path) && "POST".equalsIgnoreCase(method);
    }




    private boolean isPermitAll() {
        // Vérifiez si la classe ou la méthode est annotée avec @PermitAll
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

    private Uni<Response> abortRequest(String message) {
        return Uni.createFrom().item(
                Response.status(Response.Status.UNAUTHORIZED)
                        .entity(ApiResponseBuilder.failure(message, Response.Status.UNAUTHORIZED))
                        .build()
        );
    }
}