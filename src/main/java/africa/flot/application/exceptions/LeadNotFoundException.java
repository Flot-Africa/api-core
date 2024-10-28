package africa.flot.application.exceptions;

public class LeadNotFoundException extends RuntimeException {
    public LeadNotFoundException(String message) {
        super(message);
    }
}
