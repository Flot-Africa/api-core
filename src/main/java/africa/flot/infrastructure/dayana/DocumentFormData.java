package africa.flot.infrastructure.dayana;

import org.jboss.resteasy.reactive.RestForm;

import java.io.File;

public class DocumentFormData {

    @RestForm
    public File idDocumentFront;

    @RestForm
    public File idDocumentBack;

    @RestForm
    public String documentType;

    @RestForm
    public String verificationsToApply;
}
