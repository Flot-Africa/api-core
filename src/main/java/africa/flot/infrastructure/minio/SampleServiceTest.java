/*
package africa.flot.infrastructure.minio;

import io.minio.MinioClient;
import io.minio.messages.Bucket;
import jakarta.inject.Inject;

import java.io.InputStream;
import java.util.List;

public class SampleServiceTest {

    @Inject
    MinioClient defaultMinioClient;

    public static void main(String[] args) {

        MinioService minioService = new MinioService();

        try (InputStream inputStream = minioService.getFile("flotkyb","favicon.png")) {
            // mare le projet et partage ton ecran depuis intelij
            int data = inputStream.read();

            while (data != -1) {
                System.out.print((char) data);
                data = inputStream.read();
            }
        } catch (Exception e) {
            e.printStackTrace(); // Ceci affichera la stack trace complète
            throw new RuntimeException("Erreur générale lors de la récupération de l'objet: " + e.getMessage(), e);
        }
    }

    public void testerConnexionMinIO() {
        try {
            List<Bucket> buckets = defaultMinioClient.listBuckets();
            for (Bucket bucket : buckets) {
                System.out.println(bucket.name());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
*/
