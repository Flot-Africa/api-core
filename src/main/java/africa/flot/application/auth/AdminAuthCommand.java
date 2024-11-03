package africa.flot.application.auth;

public record AdminAuthCommand(String email, String apiKey, String sessionId) {}