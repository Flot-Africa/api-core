package africa.flot.infrastructure.interfaces.rest;


import africa.flot.application.command.subscriberCommande.CreerSubscriberCommande;
import africa.flot.application.command.subscriberCommande.ModifierSubscriberCommande;
import africa.flot.infrastructure.interfaces.facade.usecase.SubscriberUseCaseFacade;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;

@Path("/subscriptions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SubscripteurResource {

    @Inject
    SubscriberUseCaseFacade subscriberUseCaseFacade;

    @POST
    @PermitAll
    public Uni<Response> creerSubscriber(@Valid @RequestBody CreerSubscriberCommande commande) {
        return subscriberUseCaseFacade.creer(commande)
                .onItem().transform(unused -> Response.status(Response.Status.CREATED)
                        .entity("{\"message\": \"Subscriber created successfully\", \"telephone\": \"" + commande.getTelephone() + "\"}")
                        .type(MediaType.APPLICATION_JSON)
                        .build());
    }


    @PUT
    @PermitAll
    @Path("/modifier")
    public Uni<Response> modifierSubscriber(@Valid @RequestBody ModifierSubscriberCommande commande) {
        return subscriberUseCaseFacade.modifier(commande)
                .onItem().transform(unused -> Response.ok()
                        .entity("{\"message\": \"Subscriber modified successfully\", \"telephone\": \"" + commande.getTelephone() + "\"}")
                        .type(MediaType.APPLICATION_JSON)
                        .build());
    }

}
