package africa.flot.application.dto.query;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UnpaidKPIs {
    // État global
    private BigDecimal totalUnpaidAmount;
    private Integer unpaidDriversCount;
    private Double unpaidRate;  // Pourcentage

    // Ancienneté des impayés
    private AgeRanges ageRanges;

    // Recouvrement
    private RecoveryStats recoveryStats;

    // Relances
    private ReminderStats reminderStats;

    @Getter
    @Setter
    public static class AgeRanges {
        private BigDecimal lessThan7Days;
        private BigDecimal between7And30Days;
        private BigDecimal moreThan30Days;
    }

    @Getter
    @Setter
    public static class RecoveryStats {
        private BigDecimal amountRecoveredThisMonth;
        private Double regularizationPercentage;
        private Integer averageDaysToRegularization;
    }

    @Getter
    @Setter
    public static class ReminderStats {
        private Double percentageWithoutReminder;
        private Double percentageWhatsappSent;
        private ReminderDistribution distribution;
    }

    @Getter
    @Setter
    public static class ReminderDistribution {
        private Integer none;
        private Integer firstReminder;
        private Integer secondReminder;
        private Integer phoneCall;
        private Integer finalReminder;
    }
}
