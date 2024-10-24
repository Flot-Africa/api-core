package africa.flot.infrastructure.interfaces.rest;

import africa.flot.application.auth.AdminAuthCommand;
import africa.flot.application.auth.SubscriberAuthCommand;
import africa.flot.infrastructure.security.AuthService;
import africa.flot.infrastructure.util.ApiResponseBuilder;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
}