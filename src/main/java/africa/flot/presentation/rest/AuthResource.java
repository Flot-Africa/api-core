package africa.flot.presentation.rest;

import africa.flot.application.command.AuthCommand;
import africa.flot.infrastructure.security.AuthService;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @POST
    @PermitAll
    @Path("/login")
    @WithSession
    public Uni<Response> login(AuthCommand command) {
        return authService.authenticate(command.phone(), command.password())
                .onItem().transformToUni(authenticated -> {
                    if (authenticated) {
                        return authService.generateJWT(command.phone())
                                .onItem().transform(jwt -> Response.ok().entity(jwt).build());
                    } else {
                        return Uni.createFrom().item(Response.status(Response.Status.UNAUTHORIZED).build());
                    }
                });
    }
}