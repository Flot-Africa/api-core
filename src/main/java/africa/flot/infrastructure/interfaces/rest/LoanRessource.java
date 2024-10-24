package africa.flot.infrastructure.interfaces.rest;

import africa.flot.infrastructure.interfaces.facade.query.LoanQueryFacade;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/loans")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LoanRessource {

    @Inject
    LoanQueryFacade loanQueryFacade;

    // Endpoint pour récupérer un prêt par ID
    @GET
    @Path("/{id}")
    public Uni<Response> getLoanById(@PathParam("id") UUID id) {
        return loanQueryFacade.findById(id)
                .onItem().transform(loanDetail -> {
                    if (loanDetail != null) {
                        return Response.ok(loanDetail).build();
                    } else {
                        return Response.status(Response.Status.NOT_FOUND).build();
                    }
                });
    }
}
