package africa.flot.infrastructure.service.dayana;

import africa.flot.application.dto.query.DanayaVerificationResult;
import africa.flot.application.dto.query.KYBStatus;
import africa.flot.domain.model.DanayaVerificationResults;
import africa.flot.domain.model.KYBDocuments;
import africa.flot.infrastructure.logging.LoggerUtil;
import africa.flot.infrastructure.minio.MinioService;
import africa.flot.infrastructure.repository.DanayaVerificationRepository;
import africa.flot.infrastructure.repository.KYBRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class for handling Danaya verification processes.
 */
@ApplicationScoped
public class DanayaService {

    @Inject
    LoggerUtil logger;

    @Inject
    KYBRepository kybRepository;

    @Inject
    MinioService minioService;

    @Inject
    DanayaVerificationRepository danayaVerificationRepository;

    @Inject
    DanayaApiClient danayaApiClient;

    @Inject
    VerificationPoller verificationPoller;

    /**
     * Verifies ID documents with polling.
     *
     * @param bucketName the name of the bucket where the images are stored
     * @param frontImageName the name of the front image file
     * @param backImageName the name of the back image file
     * @param leadId the ID of the lead
     * @return a Uni containing the verification result
     */
    @WithSession
    public Uni<DanayaVerificationResult> verifyIdDocumentWithPolling(String bucketName, String frontImageName, String backImageName, UUID leadId) {
        return danayaVerificationRepository.findByLeadId(leadId)
                .flatMap(optionalVerification -> {
                    if (optionalVerification.isPresent() && "Vérification complète".equals(optionalVerification.get().getStatus())) {
                        logger.danayaInfo(String.format("Lead déjà vérifié [leadId=%s]. Récupération du statut de vérification actuel.", leadId));
                        return danayaApiClient.getVerificationStatus(optionalVerification.get().getId())
                                .flatMap(statusResult -> {
                                    if (!"Vérification complète".equals(statusResult.getStatus()) && !statusResult.isValid()) {
                                        logger.danayaDebug(String.format("Le statut du lead [leadId=%s] a changé ou n'est plus valide.", leadId));
                                    }
                                    return Uni.createFrom().item(statusResult);
                                });
                    }

                    logger.danayaInfo(String.format("Démarrage vérification documents [bucket=%s, front=%s, back=%s, leadId=%s]", bucketName, frontImageName, backImageName, leadId));

                    return verifyIdDocument(bucketName, frontImageName, backImageName)
                            .flatMap(initialResponse -> {
                                String verificationId = initialResponse.getString("id");
                                logger.danayaDebug("Initial response: " + initialResponse.encodePrettily());
                                if (verificationId == null || verificationId.isEmpty()) {
                                    String errorMessage = String.format("ID de vérification manquant pour le lead [leadId=%s].", leadId);
                                    logger.error(errorMessage);
                                    return Uni.createFrom().failure(new RuntimeException(errorMessage));
                                }
                                return verificationPoller.pollVerificationStatus(UUID.fromString(verificationId))
                                        .flatMap(result -> updateKYBStatus(result, leadId));
                            })
                            .onFailure().invoke(error -> logVerificationFailure(bucketName, error));
                })
                .onFailure().invoke(error -> logger.error("Erreur lors de la vérification des documents", error));
    }

    /**
     * Updates the KYB status based on the verification result.
     *
     * @param result the verification result
     * @param leadId the ID of the lead
     * @return a Uni containing the updated verification result
     */
    @WithTransaction
    protected Uni<DanayaVerificationResult> updateKYBStatus(DanayaVerificationResult result, UUID leadId) {
        return kybRepository.findByLeadId(leadId)
                .flatMap(optionalKybDoc -> {
                    KYBDocuments kybDoc = optionalKybDoc.orElseGet(() -> createNewKYBDocument(leadId));
                    updateKYBDocumentFromResult(kybDoc, result);
                    return kybRepository.persistAndFlush(kybDoc)
                            .flatMap(savedDoc -> saveVerificationResult(result, leadId))
                            .map(savedResult -> logKYBUpdateSuccess(leadId, kybDoc, result));
                });
    }

    /**
     * Creates a new KYB document for the given lead ID.
     *
     * @param leadId the ID of the lead
     * @return a new KYB document
     */
    private KYBDocuments createNewKYBDocument(UUID leadId) {
        KYBDocuments newDoc = new KYBDocuments();
        newDoc.setId(UUID.randomUUID());
        newDoc.setLeadId(leadId);
        return newDoc;
    }

    /**
     * Logs the success of the KYB update.
     *
     * @param leadId the ID of the lead
     * @param kybDoc the KYB document
     * @param result the verification result
     * @return the verification result
     */
    private DanayaVerificationResult logKYBUpdateSuccess(UUID leadId, KYBDocuments kybDoc, DanayaVerificationResult result) {
        logger.danayaInfo(String.format("KYB mis à jour [leadId=%s, progression=%d]", leadId, kybDoc.getCniProgressionVerification()));
        return result;
    }

    /**
     * Logs the failure of the verification process.
     *
     * @param bucketName the name of the bucket
     * @param error the error that occurred
     */
    private void logVerificationFailure(String bucketName, Throwable error) {
        logger.error("Échec de la vérification des documents", error);
        logger.auditAction("SYSTEM", "DOCUMENT_UPLOAD_FAILED", String.format("Échec upload documents [bucket=%s, error=%s]", bucketName, error.getMessage()));
    }

    /**
     * Saves the verification result.
     *
     * @param result the verification result
     * @param leadId the ID of the lead
     * @return a Uni representing the completion of the save operation
     */
    @WithTransaction
    public Uni<Void> saveVerificationResult(DanayaVerificationResult result, UUID leadId) {
        logger.danayaDebug("Enregistrement du résultat de vérification pour leadId=" + leadId);

        return danayaVerificationRepository.findByLeadId(leadId)
                .flatMap(optionalExistingResult -> {
                    DanayaVerificationResults verificationResult = getDanayaVerificationResults(leadId, optionalExistingResult);
                    verificationResult.updateFromVerificationResult(result);
                    return verificationResult.persistAndFlush()
                            .onItem().invoke(() -> logger.danayaInfo("Résultat de vérification enregistré pour leadId=" + leadId))
                            .onFailure().invoke(e -> logger.error("Échec de l'enregistrement du résultat de vérification pour leadId=" + leadId, e))
                            .replaceWith(Uni.createFrom().voidItem());
                });
    }

    /**
     * Retrieves or creates a new Danaya verification result.
     *
     * @param leadId the ID of the lead
     * @param optionalExistingResult an optional existing verification result
     * @return the verification result
     */
    private static DanayaVerificationResults getDanayaVerificationResults(UUID leadId, Optional<DanayaVerificationResults> optionalExistingResult) {
        return optionalExistingResult.orElseGet(() -> {
            DanayaVerificationResults verificationResult = new DanayaVerificationResults();
            verificationResult.setLeadId(leadId);
            return verificationResult;
        });
    }

    /**
     * Verifies ID documents by uploading them to the Danaya API.
     *
     * @param bucketName the name of the bucket where the images are stored
     * @param frontImageName the name of the front image file
     * @param backImageName the name of the back image file
     * @return a Uni containing the response from the Danaya API
     */
    public Uni<JsonObject> verifyIdDocument(String bucketName, String frontImageName, String backImageName) {
        return retrieveFilesFromMinio(bucketName, frontImageName, backImageName)
                .flatMap(paths -> danayaApiClient.uploadIdDocuments(paths.getItem1(), paths.getItem2()));
    }

    /**
     * Retrieves files from Minio storage.
     *
     * @param bucketName the name of the bucket
     * @param frontImageName the name of the front image file
     * @param backImageName the name of the back image file
     * @return a Uni containing a tuple of paths to the retrieved files
     */
    private Uni<Tuple2<Path, Path>> retrieveFilesFromMinio(String bucketName, String frontImageName, String backImageName) {
        return Uni.combine().all().unis(
                minioService.getFile(bucketName, frontImageName, "/tmp/" + frontImageName),
                minioService.getFile(bucketName, backImageName, "/tmp/" + backImageName)
        ).asTuple();
    }

    /**
     * Updates the KYB document based on the verification result.
     *
     * @param kybDoc the KYB document
     * @param result the verification result
     */
    private void updateKYBDocumentFromResult(KYBDocuments kybDoc, DanayaVerificationResult result) {
        if (result.isValid()) {
            kybDoc.setCniUploadee(true);
            kybDoc.setCniProgressionVerification(result.isValid() ? 100 : 0);
            logger.danayaInfo("Documents KYB validés");
        }
    }

    /**
     * Retrieves the KYB status for a given lead ID.
     *
     * @param leadId the ID of the lead
     * @return a Uni containing the KYB status
     */
    public Uni<KYBStatus> getKYBStatus(UUID leadId) {
        return kybRepository.findByLeadId(leadId)
                .flatMap(optionalKybDoc -> {
                    if (optionalKybDoc.isPresent()) {
                        KYBDocuments kyb = optionalKybDoc.get();
                        return isKYBVerified(leadId)
                                .map(isVerified -> KYBStatus.of(isVerified, kyb.getCniProgressionVerification(),
                                        isVerified ? "Vérification KYB complète" : "Vérification KYB en cours"));
                    }
                    return Uni.createFrom().item(KYBStatus.of(false, 0, "Aucune vérification KYB trouvée"));
                });
    }

    /**
     * Checks if the KYB is verified for a given lead ID.
     *
     * @param leadId the ID of the lead
     * @return a Uni containing a boolean indicating if the KYB is verified
     */
    public Uni<Boolean> isKYBVerified(UUID leadId) {
        return kybRepository.findByLeadId(leadId)
                .map(optionalKyb -> optionalKyb.map(kyb -> kyb.getCniProgressionVerification() == 100).orElse(false));
    }
}