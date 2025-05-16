package africa.flot.domain.model.enums;

public enum LoanStatus {
    ACTIVE,           // Prêt actif avec paiements en cours
    COMPLETED,        // Prêt entièrement remboursé
    DEFAULTED,        // Prêt en défaut (trop d'impayés)
    SUSPENDED,        // Prêt suspendu temporairement
    CANCELLED         // Prêt annulé
}