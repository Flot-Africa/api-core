package africa.flot.presentation.rest;

import africa.flot.application.command.CreateSubscriberCommand;
import africa.flot.application.command.SubscriberCommandHandler;
import africa.flot.application.query.SubscriberQueryService;
import africa.flot.domain.model.enums.SubscriberStatus;

import java.util.Optional;
import java.util.UUID;

import africa.flot.presentation.util.ApiResponseBuilder;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

@Path("/subscribers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SubscriberResource {

    @Inject
    SubscriberCommandHandler commandHandler;

    @Inject
    SubscriberQueryService queryService;

    private static final Logger LOG = Logger.getLogger(ApiResponseBuilder.class);

    @POST
    @RolesAllowed({"ADMIN"})
    public Uni<Response> createSubscriber(@Valid CreateSubscriberCommand command) {
        LOG.info("createSubscriber: Received command to create subscriber: " + command);
        return commandHandler.handle(command)
                .onItem().transform(id -> {
                    LOG.info("createSubscriber: Subscriber created with ID: " + id);
                    return ApiResponseBuilder.success(id);
                })
                .onFailure().recoverWithItem(ex -> {
                    LOG.error("createSubscriber: Error occurred while creating subscriber: " + ex.getMessage(), ex);
                    return ApiResponseBuilder.failure("An error occurred while creating the subscriber: " + ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
                });
    }


    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "SUBSCRIBER"})
    public Uni<Response> getSubscriber(@PathParam("id") UUID id) {
        LOG.info("getSubscriber: Fetching subscriber with ID: " + id);
        return queryService.getSubscriberById(id)
                .onItem().transform(subscriber -> Optional.ofNullable(subscriber)
                        .map(s -> {
                            LOG.info("getSubscriber: Subscriber found: " + s);
                            return ApiResponseBuilder.success(s);
                        })
                        .orElseGet(() -> {
                            LOG.warn("getSubscriber: Subscriber not found for ID: " + id);
                            return ApiResponseBuilder.failure("Subscriber not found for ID: " + id, Response.Status.NOT_FOUND);
                        }))
                .onFailure().recoverWithItem(ex -> {
                    LOG.error("getSubscriber: Error occurred while retrieving subscriber: " + ex.getMessage(), ex);
                    return ApiResponseBuilder.failure("An error occurred while retrieving the subscriber: " + ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
                });
    }

    @GET
    @RolesAllowed({"ADMIN"})
    public Uni<Response> getAllSubscribers() {
        LOG.info("getAllSubscribers: Fetching all subscribers");
        return queryService.getAllSubscribers()
                .onItem().transform(subscribers -> {
                    LOG.info("getAllSubscribers: Retrieved all subscribers");
                    return ApiResponseBuilder.success(subscribers);
                })
                .onFailure().recoverWithItem(ex -> {
                    LOG.error("getAllSubscribers: Error occurred while retrieving all subscribers: " + ex.getMessage(), ex);
                    return ApiResponseBuilder.failure("An error occurred while retrieving all subscribers: " + ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
                });
    }

    @PUT
    @Path("/{id}/update-status")
    @RolesAllowed({"ADMIN"})
    public Uni<Response> updateSubscriberStatus(@PathParam("id") UUID id, @QueryParam("status") SubscriberStatus status) {
        LOG.info("updateSubscriberStatus: Updating status for subscriber with ID: " + id + " to status: " + status);
        return commandHandler.updateSubscriberStatus(id, status)
                .onItem().transform(v -> {
                    LOG.info("updateSubscriberStatus: Status updated successfully for subscriber with ID: " + id);
                    return ApiResponseBuilder.success();
                })
                .onFailure().recoverWithItem(ex -> {
                    LOG.error("updateSubscriberStatus: Error occurred while updating subscriber status: " + ex.getMessage(), ex);
                    return ApiResponseBuilder.failure("An error occurred while updating subscriber status: " + ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
                });
    }

    @PUT
    @Path("/{id}/validate")
    @RolesAllowed({"ADMIN"})
    public Uni<Response> validateSubscriber(@PathParam("id") UUID id) {
        LOG.info("validateSubscriber: Validating subscriber with ID: " + id);
        return commandHandler.validateSubscriber(id)
                .onItem().transform(v -> {
                    LOG.info("validateSubscriber: Subscriber validated successfully with ID: " + id);
                    return ApiResponseBuilder.success();
                })
                .onFailure().recoverWithItem(ex -> {
                    LOG.error("validateSubscriber: Error occurred while validating subscriber: " + ex.getMessage(), ex);
                    return ApiResponseBuilder.failure("An error occurred while validating the subscriber: " + ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
                });
    }

    @PUT
    @Path("/{id}/deactivate-account")
    @RolesAllowed({"ADMIN"})
    public Uni<Response> deactivateAccount(@PathParam("id") UUID id) {
        LOG.info("deactivateAccount: Deactivating account for subscriber with ID: " + id);
        return commandHandler.deactivateAccount(id)
                .onItem().transform(v -> {
                    LOG.info("deactivateAccount: Account deactivated successfully for subscriber with ID: " + id);
                    return ApiResponseBuilder.success();
                })
                .onFailure().recoverWithItem(ex -> {
                    LOG.error("deactivateAccount: Error occurred while deactivating account: " + ex.getMessage(), ex);
                    return ApiResponseBuilder.failure("An error occurred while deactivating the account: " + ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
                });
    }
}