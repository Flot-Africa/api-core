package africa.flot.domain.model;

import africa.flot.domain.model.enums.ReminderType;
import africa.flot.domain.model.enums.ReminderLevel;
import africa.flot.domain.model.enums.ReminderStatus;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_reminders")
@Getter
@Setter
public class LoanReminder extends PanacheEntityBase {

    @Id
    private UUID id;

    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    // Relation avec le prêt
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", insertable = false, updatable = false)
    private FlotLoan loan;

    // Type et niveau de relance
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ReminderType type; // WHATSAPP, SMS, PHONE_CALL, EMAIL

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private ReminderLevel level; // FIRST, SECOND, THIRD, FINAL

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReminderStatus status = ReminderStatus.SENT;

    // Contenu de la relance
    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "recipient_phone")
    private String recipientPhone;

    @Column(name = "recipient_email")
    private String recipientEmail;

    // Dates importantes
    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt; // Quand le chauffeur a répondu

    @Column(name = "next_reminder_due")
    private LocalDateTime nextReminderDue; // Quand envoyer la prochaine relance

    // Responses et feedback
    @Column(name = "acknowledged")
    private Boolean acknowledged = false;

    @Column(name = "response_message", length = 500)
    private String responseMessage; // Réponse du chauffeur

    // Informations sur l'impayé au moment de la relance
    @Column(name = "overdue_amount", precision = 10, scale = 2)
    private java.math.BigDecimal overdueAmount;

    @Column(name = "days_overdue")
    private Integer daysOverdue;

    @Column(name = "weeks_overdue")
    private Integer weeksOverdue;

    // Référence externe (ID WhatsApp, SMS, etc.)
    @Column(name = "external_reference")
    private String externalReference;

    // Coût de l'envoi (pour SMS/WhatsApp)
    @Column(name = "sending_cost", precision = 5, scale = 2)
    private java.math.BigDecimal sendingCost;

    // Métadonnées
    @Column(name = "created_by")
    private String createdBy; // Système automatique ou utilisateur manuel

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (sentAt == null) sentAt = LocalDateTime.now();
        calculateNextReminderDue();
    }

    // Calcul automatique de la prochaine relance
    private void calculateNextReminderDue() {
        if (level == ReminderLevel.FINAL) {
            nextReminderDue = null; // Pas de relance après la finale
            return;
        }

        // Délais entre les relances selon le niveau
        switch (level) {
            case FIRST -> nextReminderDue = sentAt.plusDays(3); // 3 jours après la première
            case SECOND -> nextReminderDue = sentAt.plusDays(5); // 5 jours après la deuxième
            case THIRD -> nextReminderDue = sentAt.plusDays(7); // 7 jours après la troisième
            default -> nextReminderDue = null;
        }
    }

    // Marquer comme acquitté (chauffeur a répondu)
    public void acknowledge(String responseMessage) {
        this.acknowledged = true;
        this.acknowledgedAt = LocalDateTime.now();
        this.responseMessage = responseMessage;
        this.status = ReminderStatus.ACKNOWLEDGED;
    }

    // Marquer comme échoué
    public void markAsFailed(String reason) {
        this.status = ReminderStatus.FAILED;
        this.responseMessage = reason;
    }

    // Vérifier si c'est le moment d'envoyer la prochaine relance
    public boolean isNextReminderDue() {
        return nextReminderDue != null &&
                LocalDateTime.now().isAfter(nextReminderDue) &&
                !acknowledged;
    }

    // Obtenir le niveau suivant de relance
    public ReminderLevel getNextLevel() {
        return switch (level) {
            case FIRST -> ReminderLevel.SECOND;
            case SECOND -> ReminderLevel.THIRD;
            case THIRD -> ReminderLevel.FINAL;
            case FINAL -> null; // Pas de niveau après final
        };
    }

    // Calculer le délai depuis l'envoi
    public long getHoursSinceSent() {
        return java.time.temporal.ChronoUnit.HOURS.between(sentAt, LocalDateTime.now());
    }

    // Vérifier si la relance est expirée (pas de réponse après X temps)
    public boolean isExpired() {
        long hoursLimit = switch (level) {
            case FIRST -> 72; // 3 jours
            case SECOND -> 120; // 5 jours
            case THIRD -> 168; // 7 jours
            case FINAL -> 336; // 14 jours
        };
        return getHoursSinceSent() > hoursLimit && !acknowledged;
    }
}