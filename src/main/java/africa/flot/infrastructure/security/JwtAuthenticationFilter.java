package africa.flot.infrastructure.security;

import africa.flot.infrastructure.repository.TokenBlacklistRepository;
import africa.flot.infrastructure.util.ApiResponseBuilder;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import java.lang.reflect.Method;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthenticationFilter implements ContainerRequestFilter {

    @Inject
    TokenBlacklistRepository tokenBlacklistRepository;

    @Context
    ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Vérifie si l'endpoint en cours est annoté avec @PermitAll
        if (isPermitAll()) {
            // Si l'endpoint est accessible sans authentification, on continue sans vérifier le token
            return;
        }

        String token = extractToken(requestContext);
        if (token == null) {
            abortRequest(requestContext, "Token manquant");
            return;
        }

        tokenBlacklistRepository.isTokenBlacklisted(token)
                .onItem().transform(isBlacklisted -> {
                    if (isBlacklisted) {
                        abortRequest(requestContext, "Token invalidé");
                        return null;
                    }
                    // Continuer le processus d'authentification si le token n'est pas dans la liste
                    return null;
                }).subscribe().with(unused -> {});
    }

    private boolean isPermitAll() {
        // Récupère la classe et la méthode en cours
        Class<?> resourceClass = resourceInfo.getResourceClass();
        Method resourceMethod = resourceInfo.getResourceMethod();

        // Vérifie si la méthode ou la classe a l'annotation @PermitAll
        return resourceClass.isAnnotationPresent(PermitAll.class) || resourceMethod.isAnnotationPresent(PermitAll.class);
    }

    private String extractToken(ContainerRequestContext requestContext) {
        String authHeader = requestContext.getHeaderString("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private void abortRequest(ContainerRequestContext requestContext, String message) {
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity(ApiResponseBuilder.failure(message, Response.Status.UNAUTHORIZED))
                .build());
    }
}
