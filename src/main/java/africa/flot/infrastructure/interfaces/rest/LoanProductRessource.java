/*
package africa.flot.infrastructure.interfaces.rest;

import africa.flot.application.dto.command.loanproduit.LoanProductRequest;
import africa.flot.application.exceptions.ErrorResponse;
import africa.flot.domain.model.exception.BusinessException;
import africa.flot.infrastructure.service.LoanProductServiceImpl;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/loanproducts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LoanProductRessource {

    @Inject
    LoanProductServiceImpl loanProductService;

    @POST
    public Uni<Response> createLoanProduct(LoanProductRequest request) {
        return loanProductService.createLoanProduct(request)
                .onItem().transform(response -> {
                    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                        return Response.ok(response.readEntity(String.class)).build();
                    } else {
                        return Response.status(response.getStatus())
                                .entity(response.readEntity(String.class))
                                .build();
                    }
                })
                .onFailure().recoverWithItem(error -> {
                    if (error instanceof BusinessException) {
                        return Response.status(Response.Status.BAD_REQUEST)
                                .entity(new ErrorResponse(error.getMessage()))
                                .build();
                    }
                    return Response.serverError()
                            .entity(new ErrorResponse("Une erreur est survenue"))
                            .build();
                });
    }
}

*/
