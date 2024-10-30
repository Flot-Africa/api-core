package africa.flot.infrastructure.minio;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
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

    @Inject
    io.vertx.mutiny.core.Vertx vertx;

    public Uni<Path> getFile(String bucketName, String objectName, String outputPath) {
        return vertx.executeBlocking(Uni.createFrom().item(() -> {
            try (InputStream fileStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            )) {
                Path filePath = Path.of(outputPath);
                Files.copy(fileStream, filePath, StandardCopyOption.REPLACE_EXISTING);
                LOG.infof("Fichier récupéré depuis Minio : %s [path=%s]", objectName, filePath);
                return filePath;
            } catch (Exception e) {
                LOG.errorf("Erreur lors de la récupération du fichier %s depuis Minio : %s", objectName, e.getMessage());
                throw new RuntimeException("Erreur lors de la récupération du fichier depuis Minio : " + e.getMessage(), e);
            }
        }));
    }
}
