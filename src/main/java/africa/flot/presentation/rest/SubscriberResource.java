package africa.flot.presentation.rest;

import africa.flot.application.command.CreateSubscriberCommand;
import africa.flot.application.command.SubscriberCommandHandler;
import africa.flot.application.query.SubscriberQueryService;

import java.util.UUID;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/subscribers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SubscriberResource {

    @Inject
    SubscriberCommandHandler commandHandler;

    @Inject
    SubscriberQueryService queryService;


    @POST
    public Uni<Response> createSubscriber(CreateSubscriberCommand command) {
        return commandHandler.handle(command)
                .onItem().transform(id -> Response.status(Response.Status.CREATED).entity(id).build())
                .onFailure().recoverWithItem(ex -> Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("An error occurred while creating the subscriber: " + ex.getMessage()).build());
    }

    @GET
    @Path("/{id}")
    public Uni<Response> getSubscriber(@PathParam("id") UUID id) {
        return queryService.getSubscriberById(id)
                .onItem().transform(subscriber -> subscriber != null
                        ? Response.ok(subscriber).build()
                        : Response.status(Response.Status.NOT_FOUND)
                        .entity("Subscriber not found for ID: " + id)
                        .build())
                .onFailure().recoverWithItem(ex -> Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("An error occurred while retrieving the subscriber: " + ex.getMessage())
                        .build());
    }


    @GET
    public Uni<Response> getAllSubscribers() {
        return queryService.getAllSubscribers()
                .onItem().transform(subscribers -> Response.ok(subscribers).build());
    }

    @PUT
    @Path("/{id}/verify-kyb")
    public Uni<Response> verifyKYB(@PathParam("id") UUID id) {
        return commandHandler.verifyKYB(id)
                .onItem().transform(v -> Response.ok().build());
    }

    @PUT
    @Path("/{id}/reject-kyb")
    public Uni<Response> rejectKYB(@PathParam("id") UUID id) {
        return commandHandler.rejectKYB(id)
                .onItem().transform(v -> Response.ok().build());
    }

    @PUT
    @Path("/{id}/deactivate")
    public Uni<Response> deactivateSubscriber(@PathParam("id") UUID id) {
        return commandHandler.deactivateSubscriber(id)
                .onItem().transform(v -> Response.ok().build());
    }
}