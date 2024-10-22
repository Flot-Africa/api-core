package africa.flot.infrastructure.interfaces.rest;

import africa.flot.infrastructure.dayana.DanayaVerificationService;
import africa.flot.infrastructure.minio.MinioService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.smallrye.mutiny.Uni;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Path("/verification")
public class VerificationResource {

    @Inject
    DanayaVerificationService danayaVerificationService;

    @Inject
    MinioService minioService;

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> verifyFile(@PathParam("id") UUID id) throws Exception {

        // Obtenir le chemin de la racine du projet
        java.nio.file.Path projectRoot = Paths.get("").toAbsolutePath();

        // Définir le répertoire temporaire à la racine du projet
        java.nio.file.Path tempDirectoryPath = projectRoot.resolve("tmp");

        // Créer le répertoire tmp s'il n'existe pas
        if (!Files.exists(tempDirectoryPath)) {
            Files.createDirectories(tempDirectoryPath);
        }

        // Utiliser le répertoire tmp pour stocker le fichier récupéré depuis MinIO
        java.nio.file.Path filePath = minioService.getFile("flotkyb", "CNI-RECTO.jpeg", tempDirectoryPath.resolve("CNI-RECTO.jpeg").toString());

        // Appeler le service de vérification Danaya
        return danayaVerificationService.verifyDocument(filePath.toString())
                .onItem().transform(result -> Response.ok(result).build())
                .onFailure().recoverWithItem(e -> Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(e.getMessage())
                        .build());
    }
}
