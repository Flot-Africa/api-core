package africa.flot.infrastructure.job;

import africa.flot.domain.model.Account;
import africa.flot.infrastructure.service.JetfySmsService;
import africa.flot.infrastructure.util.PasswordGenerator;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class WelcomeSmsRetryJob {
    private static final Logger LOG = Logger.getLogger(WelcomeSmsRetryJob.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final String SMS_TEMPLATE = "Bienvenue chez Flot! Vos identifiants:\nIdentifiant: %s\nMot de passe: %s";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Inject
    JetfySmsService smsService;

    @Scheduled(every = "20m")
    @WithTransaction
    public Uni<Void> processFailedWelcomeSms() {
        return findPendingAccounts()
                .flatMap(this::processPendingAccounts)
                .onFailure().invoke(e -> LOG.errorf(e, "Erreur lors du traitement des SMS de bienvenue"))
                .ifNoItem().after(TIMEOUT).fail()
                .replaceWithVoid();
    }

    private Uni<List<Account>> findPendingAccounts() {
        return Account.<Account>find("pendingWelcomeSms = ?1", true)
                .list()
                .invoke(accounts -> LOG.infof("Trouvé %d comptes en attente de SMS de bienvenue", accounts.size()));
    }

    private Uni<List<Account>> processPendingAccounts(List<Account> accounts) {
        if (accounts.isEmpty()) {
            return Uni.createFrom().item(List.of());
        }

        List<Uni<Account>> tasks = new ArrayList<>();
        for (Account account : accounts) {
            tasks.add(processAccount(account));
        }

        return Uni.join().all(tasks).andCollectFailures();
    }

    private Uni<Account> processAccount(Account account) {
        if (account.getSmsRetryCount() >= MAX_RETRY_ATTEMPTS) {
            return handleMaxRetryReached(account);
        }

        String newPassword = PasswordGenerator.generate();
        String message = String.format(SMS_TEMPLATE, account.getUsername(), newPassword);

        return smsService.sendSMS(account.getUsername(), message, account)
                .onItem().transformToUni(response -> handleSmsResponse(account, response, newPassword))
                .onFailure().invoke(e -> LOG.errorf(e, "Erreur lors de l'envoi du SMS pour %s", account.getUsername()));
    }

    private Uni<Account> handleMaxRetryReached(Account account) {
        LOG.warnf("Désactivation du compte %s après %d tentatives d'envoi de SMS échouées",
                account.getUsername(), MAX_RETRY_ATTEMPTS);

        account.setActive(false);
        account.setPendingWelcomeSms(false);

        return account.<Account>persistAndFlush()
                .invoke(() -> LOG.infof("Compte %s désactivé avec succès", account.getUsername()));
    }

    private Uni<Account> handleSmsResponse(Account account, Response response, String newPassword) {
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            return handleSuccessfulSms(account, newPassword);
        } else {
            return handleFailedSms(account, response.getStatus());
        }
    }

    private Uni<Account> handleSuccessfulSms(Account account, String newPassword) {
        account.setPendingWelcomeSms(false);
        account.setPasswordHash(BcryptUtil.bcryptHash(newPassword));
        account.setSmsRetryCount(0);

        LOG.infof("SMS de bienvenue envoyé avec succès pour %s après %d tentatives",
                account.getUsername(), account.getSmsRetryCount() + 1);

        return account.persistAndFlush();
    }

    private Uni<Account> handleFailedSms(Account account, int statusCode) {
        account.setSmsRetryCount(account.getSmsRetryCount() + 1);

        LOG.warnf("Échec de l'envoi du SMS de bienvenue pour %s (tentative %d/%d, status: %d)",
                account.getUsername(), account.getSmsRetryCount(), MAX_RETRY_ATTEMPTS, statusCode);

        return account.persistAndFlush();
    }
}