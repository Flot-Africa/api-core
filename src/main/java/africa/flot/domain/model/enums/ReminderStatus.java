package africa.flot.domain.model.enums;

public enum ReminderStatus {
    SENT,                 // Relance envoyée
    DELIVERED,            // Relance délivrée
    READ,                 // Relance lue
    ACKNOWLEDGED,         // Chauffeur a répondu
    FAILED,               // Échec d'envoi
    EXPIRED               // Relance expirée sans réponse
}
