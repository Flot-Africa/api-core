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

    @WithTransaction
    protected Uni<DanayaVerificationResult> updateKYBStatus(DanayaVerificationResult result, UUID leadId) {
        return kybRepository.findByLeadId(leadId)
                .flatMap(optionalKybDoc -> {
                    if (optionalKybDoc.isPresent()) {
                        // Mise à jour du KYBDocuments existant
                        KYBDocuments kybDoc = optionalKybDoc.get();
                        updateKYBDocumentFromResult(kybDoc, result);
                        return kybRepository.persistAndFlush(kybDoc)
                                .flatMap(savedDoc -> saveVerificationResult(result, leadId))
                                .map(savedResult -> logKYBUpdateSuccess(leadId, kybDoc, result));
                    } else {
                        // Création d'un nouveau KYBDocuments
                        KYBDocuments newDoc = createNewKYBDocument(leadId);
                        updateKYBDocumentFromResult(newDoc, result);
                        return kybRepository.persistAndFlush(newDoc)
                                .flatMap(savedDoc -> saveVerificationResult(result, leadId))
                                .map(savedResult -> logKYBUpdateSuccess(leadId, newDoc, result));
                    }
                });
    }

    private KYBDocuments createNewKYBDocument(UUID leadId) {
        KYBDocuments newDoc = new KYBDocuments();
        newDoc.setId(UUID.randomUUID());
        newDoc.setLeadId(leadId);
        return newDoc;
    }

    private DanayaVerificationResult logKYBUpdateSuccess(UUID leadId, KYBDocuments kybDoc, DanayaVerificationResult result) {
        logger.danayaInfo(String.format("KYB mis à jour [leadId=%s, progression=%d]", leadId, kybDoc.getCniProgressionVerification()));
        return result;
    }

    private void logVerificationFailure(String bucketName, Throwable error) {
        logger.error("Échec de la vérification des documents", error);
        logger.auditAction("SYSTEM", "DOCUMENT_UPLOAD_FAILED", String.format("Échec upload documents [bucket=%s, error=%s]", bucketName, error.getMessage()));
    }

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

    private static DanayaVerificationResults getDanayaVerificationResults(UUID leadId, Optional<DanayaVerificationResults> optionalExistingResult) {
        return optionalExistingResult.orElseGet(() -> {
            DanayaVerificationResults verificationResult = new DanayaVerificationResults();
            verificationResult.setLeadId(leadId);
            return verificationResult;
        });
    }

    public Uni<JsonObject> verifyIdDocument(String bucketName, String frontImageName, String backImageName) {
        return retrieveFilesFromMinio(bucketName, frontImageName, backImageName)
                .flatMap(paths -> danayaApiClient.uploadIdDocuments(paths.getItem1(), paths.getItem2()));
    }

    private Uni<Tuple2<Path, Path>> retrieveFilesFromMinio(String bucketName, String frontImageName, String backImageName) {
        return Uni.combine().all().unis(
                minioService.getFile(bucketName, frontImageName, "/tmp/" + frontImageName),
                minioService.getFile(bucketName, backImageName, "/tmp/" + backImageName)
        ).asTuple();
    }

    private void updateKYBDocumentFromResult(KYBDocuments kybDoc, DanayaVerificationResult result) {
        if (result.isValid()) {
            kybDoc.setCniUploadee(true);
            kybDoc.setCniProgressionVerification(result.isValid() ? 100 : 0);
            logger.danayaInfo("Documents KYB validés");
        }
    }

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

    public Uni<Boolean> isKYBVerified(UUID leadId) {
        return kybRepository.findByLeadId(leadId)
                .map(optionalKyb -> optionalKyb.map(kyb -> kyb.getCniProgressionVerification() == 100).orElse(false));
    }
}
