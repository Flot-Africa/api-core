package africa.flot.infrastructure.client;

import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.smallrye.mutiny.Uni;

/**
 * Interface REST client réactif pour communiquer avec l’API Fineract.
 *
 * - configKey = "fineract-api" => Quarkus cherchera dans application.properties
 *   les propriétés :
 *     fineract-api/mp-rest/url=...
 *     fineract-api/mp-rest/connectTimeout=... etc.
 * - @ClientHeaderParam => insère un header "fineract-platform-tenantid: default"
 *   sur toutes les requêtes.
 */
@Path("/v1")
@RegisterRestClient(configKey = "fineract-api")
@ClientHeaderParam(name = "fineract-platform-tenantid", value = "default")
@RegisterProvider(FineractAuthenticationProvider.class)
@RegisterProvider(FineractLoggingFilter.class)
public interface FineractClient {

    /**
     * Récupère un produit de prêt par son ID.
     *
     * @param productId l'ID du produit de prêt
     * @return une réponse JSON asynchrone
     */
    @GET
    @Path("/loanproducts/{productId}")
    @Produces(MediaType.APPLICATION_JSON)
    Uni<Response> getLoanProduct(@PathParam("productId") Integer productId);

    /**
     * Récupère un client (et ses comptes) via un externalId.
     *
     * @param externalId l'externalId du client
     * @return une réponse JSON asynchrone
     */
    @GET
    @Path("/clients/external-id/{externalId}")
    @Produces(MediaType.APPLICATION_JSON)
    Uni<Response> getClientByExternalId(@PathParam("externalId") String externalId);

    /**
     * Crée un prêt.
     *
     * @param requestBody JSON contenant les champs nécessaires à la création du prêt
     * @return une réponse JSON asynchrone
     */
    @POST
    @Path("/loans")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<Response> createLoan(String requestBody);

    /**
     * Crée un client dans Fineract.
     *
     * @param requestBody JSON contenant les champs nécessaires à la création du client
     * @return une réponse JSON asynchrone
     */
    @POST
    @Path("/clients")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<Response> createClient(JsonObject requestBody);


    @GET
    @Path("/loans/external-id/{externalId}")
    Uni<Response> getLoanByExternalId(
            @PathParam("externalId") String externalId,
            @QueryParam("associations") String associations,
            @QueryParam("exclude") @Encoded String exclude  // Ajout de @Encoded pour éviter le double encodage
    );

    @GET
    @Path("/loans/external-id/{loanExternalId}/template")
    Uni<Response> getLoanTemplate(
            @PathParam("loanExternalId") String loanExternalId,
            @QueryParam("templateType") String templateType
    );

    @POST
    @Path("/loans/external-id/{loanExternalId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<Response> postLoanCommand(
            @PathParam("loanExternalId") String loanExternalId,
            @QueryParam("command") String command,
            String requestBody
    );

    @GET
    @Path("/loans/external-id/{loanExternalId}/transactions/template")
    Uni<Response> getLoanTransactionTemplate(
            @PathParam("loanExternalId") String loanExternalId,
            @QueryParam("command") String command
    );

    @POST
    @Path("/loans/external-id/{loanExternalId}/transactions")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<Response> postLoanTransaction(
            @PathParam("loanExternalId") String loanExternalId,
            @QueryParam("command") String command,
            String requestBody
    );

}