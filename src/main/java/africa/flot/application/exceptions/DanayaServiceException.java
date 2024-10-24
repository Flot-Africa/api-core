package africa.flot.application.exceptions;

public class DanayaServiceException extends RuntimeException {
    public DanayaServiceException(String message) {
        super(message);
    }

    public DanayaServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
