package africa.flot.domain.model.enums;

public enum PaymentStatus {
    PAID_IN_ADVANCE,      // Payé en avance
    PAID_ON_TIME,         // Payé à temps
    PAID_LATE_MINOR,      // Payé en retard (moins d'une semaine)
    PAID_LATE_MAJOR,      // Payé en retard (plus d'une semaine)
    PARTIAL_PAYMENT       // Paiement partiel
}