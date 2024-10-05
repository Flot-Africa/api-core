package africa.flot.presentation.rest;

import africa.flot.application.command.auth.AdminAuthCommand;
import africa.flot.application.command.auth.SubscriberAuthCommand;
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
    @Path("/subscriber/login")
    @WithSession
    public Uni<Response> loginSubscriber(SubscriberAuthCommand command) {
        return authService.authenticateSubscriber(command.phone(), command.password())
                .onItem().transformToUni(authenticated -> {
                    if (authenticated) {
                        return authService.generateSubscriberJWT(command.phone())
                                .onItem().transform(jwt -> Response.ok().entity(jwt).build());
                    } else {
                        return Uni.createFrom().item(Response.status(Response.Status.UNAUTHORIZED).build());
                    }
                });
    }

    @POST
    @PermitAll
    @Path("/admin/login")
    @WithSession
    public Uni<Response> loginAdmin(AdminAuthCommand command) {
        return authService.authenticateAdmin(command.email(), command.password())
                .onItem().transformToUni(authenticated -> {
                    if (authenticated) {
                        return authService.generateAdminJWT(command.email())
                                .onItem().transform(jwt -> Response.ok().entity(jwt).build());
                    } else {
                        return Uni.createFrom().item(Response.status(Response.Status.UNAUTHORIZED).build());
                    }
                });
    }
}