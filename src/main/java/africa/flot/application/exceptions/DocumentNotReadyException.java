package africa.flot.application.exceptions;

public class DocumentNotReadyException extends RuntimeException {
    public DocumentNotReadyException() {
        super("Document en cours d'initialisation");
    }
}
