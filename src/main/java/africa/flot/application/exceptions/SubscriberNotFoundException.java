package africa.flot.application.exceptions;

public class SubscriberNotFoundException extends RuntimeException {
    public SubscriberNotFoundException(String message) {
        super(message);
    }
}
