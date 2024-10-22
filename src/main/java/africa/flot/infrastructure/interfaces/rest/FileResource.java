package africa.flot.infrastructure.interfaces.rest;

import africa.flot.infrastructure.minio.MinioService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.nio.file.Files;

import java.nio.file.Paths;

@Path("/files")
public class FileResource {

    @Inject
    MinioService minioService;

    @GET
    @Path("/{fileName}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getFilePath(@PathParam("fileName") String fileName) {
        try {
            // Obtenir le chemin de la racine du projet
            java.nio.file.Path projectRoot = Paths.get("").toAbsolutePath();

            // Définir le répertoire temporaire à la racine du projet
            java.nio.file.Path tempDirectoryPath = projectRoot.resolve("tmp");

            // Créer le répertoire tmp s'il n'existe pas
            if (!Files.exists(tempDirectoryPath)) {
                Files.createDirectories(tempDirectoryPath);
            }

            // Utiliser le répertoire tmp pour stocker le fichier
            java.nio.file.Path filePath = minioService.getFile("flotkyb", fileName, tempDirectoryPath.resolve(fileName).toString());

            // Retourner le chemin du fichier sous forme de texte
            return Response.ok(filePath.toString()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage())
                    .build();
        }
    }
}
