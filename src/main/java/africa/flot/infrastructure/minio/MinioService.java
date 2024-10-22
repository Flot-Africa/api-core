package africa.flot.infrastructure.minio;

import io.minio.MinioClient;
import io.minio.GetObjectArgs;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@ApplicationScoped
public class MinioService {

    @Inject
    MinioClient minioClient;

    public Path getFile(String bucketName, String objectName, String outputPath) throws Exception {
        InputStream fileStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
        Path filePath = Path.of(outputPath);
        Files.copy(fileStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        return filePath;
    }
}



