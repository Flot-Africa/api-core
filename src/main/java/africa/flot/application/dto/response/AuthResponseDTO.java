package africa.flot.application.dto.response;

public record AuthResponseDTO(
        String token,
        long expiresIn,
        boolean passwordChanged,
        boolean requirePasswordChange
) {}
