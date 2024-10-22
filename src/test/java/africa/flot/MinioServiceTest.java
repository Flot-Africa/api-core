package africa.flot;

import africa.flot.infrastructure.minio.MinioService;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class MinioServiceTest {

    @Inject
    MinioService minioService;

    @Inject
    MinioClient minioClient;

    @BeforeEach
    public void setup() throws Exception {
        // Préparez un fichier test dans Minio
        minioClient.uploadObject(
                UploadObjectArgs.builder()
                        .bucket("test-bucket")
                        .object("test-image.jpg")
                        .filename("/chemin/vers/image.jpg")
                        .build()
        );
    }

    @Test
    public void testGetFile() {
        Uni<Path> fileUni = minioService.getFile("test-bucket", "test-image.jpg", "/tmp/test-image.jpg");
        Path filePath = fileUni.await().indefinitely();
        assertNotNull(filePath);
        assertTrue(Files.exists(filePath));
    }
}

