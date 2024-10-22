package africa.flot.application.usecase.document;


import africa.flot.infrastructure.dayana.DanayaVerificationService;
import africa.flot.infrastructure.minio.MinioService;
import jakarta.inject.Inject;

public class VeriflierCNI {

    @Inject
    MinioService minioService;

    @Inject
    DanayaVerificationService danayaVerificationService;
}
