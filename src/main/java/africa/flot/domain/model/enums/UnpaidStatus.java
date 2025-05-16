package africa.flot.domain.model.enums;

public enum UnpaidStatus {
    ON_TIME,              // À jour dans les paiements
    EN_RETARD,            // En retard (0-7 jours)
    RELANCE_1,            // Première relance envoyée
    RELANCE_2,            // Deuxième relance envoyée
    RELANCE_TELEPHONE,    // Relance téléphonique effectuée
    RELANCE_FINALE,       // Relance finale envoyée
    EN_COURS_REGULARISATION, // Le chauffeur a promis de payer
    DEFAUT_PAIEMENT       // Défaut de paiement confirmé
}
