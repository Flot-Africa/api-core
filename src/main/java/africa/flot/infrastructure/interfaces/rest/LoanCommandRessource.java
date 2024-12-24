/*
package africa.flot.infrastructure.interfaces.rest;


import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

@Path("/loans")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LoanCommandRessource {


    @POST
    public Response createLoan(CreateLoanCommand command) {
        try {
            String loanId = createLoanHandler.handle(command);
            return Response.status(Status.CREATED)
                    .entity(Map.of("loanId", loanId))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
}

*/
