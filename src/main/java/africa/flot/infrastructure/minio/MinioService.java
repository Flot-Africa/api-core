package africa.flot.infrastructure.minio;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@ApplicationScoped
public class MinioService {

    private static final Logger LOG = Logger.getLogger(MinioService.class);

    @Inject
    MinioClient minioClient;


    public Uni<Path> getFile(String bucketName, String objectName, String outputPath) {
        return Uni.createFrom().item(Unchecked.supplier(() -> {
            Path filePath = Path.of(outputPath);

            // Vérifier si l'objet existe sur Minio
            try {
                minioClient.statObject(StatObjectArgs.builder().bucket(bucketName).object(objectName).build());
                LOG.info("L'objet existe sur Minio : " + objectName);
            } catch (Exception e) {
                LOG.error("L'objet n'existe pas sur Minio : " + objectName, e);
                throw new RuntimeException("L'objet n'existe pas sur Minio", e);
            }

            // Télécharger le fichier depuis Minio
            try (InputStream fileStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            )) {
                Files.copy(fileStream, filePath, StandardCopyOption.REPLACE_EXISTING);
                LOG.info("Fichier récupéré depuis Minio : " + objectName + " [path=" + filePath + "]");
                return filePath;
            } catch (Exception e) {
                LOG.error("Erreur lors de la récupération du fichier depuis Minio", e);
                throw new RuntimeException("Erreur lors de la récupération du fichier depuis Minio", e);
            }
        })).runSubscriptionOn(Infrastructure.getDefaultExecutor());
    }


}
