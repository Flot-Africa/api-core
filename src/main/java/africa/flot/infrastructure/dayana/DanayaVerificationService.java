package africa.flot.infrastructure.dayana;

import africa.flot.application.exceptions.DanayaServiceException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;

@ApplicationScoped
public class DanayaVerificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DanayaVerificationService.class);

    @RestClient
    DanayaClient danayaClient;

    @ConfigProperty(name = "quarkus.rest-client.danaya-api.api-key")
    String apiKey;

    @ConfigProperty(name = "quarkus.rest-client.danaya-api.api-secret")
    String apiSecret;

    public Uni<Response> verifyDocument(String filePath) {
        return Uni.createFrom().item(Unchecked.supplier(() -> {
                    // Validation du chemin du fichier
                    if (filePath == null || filePath.trim().isEmpty()) {
                        throw new IllegalArgumentException("Le chemin du fichier ne peut pas être null ou vide");
                    }

                    // Vérification de l'existence du fichier
                    File file = new File(filePath);
                    if (!file.exists()) {
                        try {
                            throw new FileNotFoundException("Le fichier n'existe pas : " + filePath);
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if (!file.canRead()) {
                        throw new SecurityException("Impossible de lire le fichier : " + filePath);
                    }

                    // Préparation des données
                    DocumentFormData formData = new DocumentFormData();
                    formData.idDocumentFront = file;
                    formData.documentType = "CNI";
                    formData.verificationsToApply = "DB_CHECK";

                    return formData;
                }))
                .chain(formData -> danayaClient.uploadDocument(apiKey, apiSecret, formData))
                .onFailure().recoverWithItem(Unchecked.function(throwable -> {
                    LOGGER.error("Erreur lors de la vérification du document", throwable);

                    // Gestion spécifique selon le type d'erreur
                    if (throwable instanceof FileNotFoundException) {
                        throw new DanayaServiceException("Fichier non trouvé: " + filePath, throwable);
                    }
                    if (throwable instanceof SecurityException) {
                        throw new DanayaServiceException("Erreur d'accès au fichier: " + filePath, throwable);
                    }
                    if (throwable instanceof IllegalArgumentException) {
                        throw new DanayaServiceException("Paramètre invalide", throwable);
                    }
                    if (throwable instanceof RestClientDefinitionException) {
                        throw new DanayaServiceException("Erreur de communication avec l'API Danaya", throwable);
                    }

                    // Erreur générique
                    throw new DanayaServiceException("Erreur inattendue lors de la vérification du document", throwable);
                }));
    }
}