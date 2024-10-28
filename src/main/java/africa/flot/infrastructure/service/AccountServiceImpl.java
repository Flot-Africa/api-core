package africa.flot.infrastructure.service;

import africa.flot.application.ports.AccountService;
import africa.flot.domain.model.Account;
import africa.flot.domain.model.Lead;
import africa.flot.domain.model.Package;
import africa.flot.infrastructure.util.PasswordGenerator;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@ApplicationScoped
public class AccountServiceImpl implements AccountService {

    @Inject
    JetfySmsService smsService;

    @Override
    @WithSession
    public Uni<Response> subscribeToPackage(UUID leadId, UUID packageId) {
        return Lead.findById(leadId)
                .map(leadEntity -> (Lead) leadEntity)
                .flatMap(lead -> {
                    if (lead == null) {
                        return Uni.createFrom().failure(new IllegalStateException("Lead introuvable"));
                    }
                    return Package.findById(packageId)
                            .map(packageEntity -> (Package) packageEntity)
                            .flatMap(packageEntity -> {
                                if (packageEntity == null) {
                                    return Uni.createFrom().failure(new IllegalStateException("Forfait introuvable"));
                                }

                                // Générer un mot de passe et récupérer le numéro de téléphone pour le nom d'utilisateur
                                String generatedPassword = PasswordGenerator.generate();
                                String username = lead.getPhoneNumber();

                                // Créer un compte pour le lead
                                Account account = new Account();
                                account.setLead(lead);
                                account.setSubscribedPackage(packageEntity);
                                account.setUsername(username);
                                account.setPasswordHash(BcryptUtil.bcryptHash(generatedPassword));
                                account.setActive(true);

                                String formattedPhoneNumber = formatPhoneNumber(username);

                                String message = String.format("Votre compte Flot a été créé. Identifiant : %s, Mot de passe : %s", formattedPhoneNumber, generatedPassword);

                                return smsService.sendSMS(username, message)
                                        .flatMap(response -> {
                                            if (response.getStatus() == 200) {
                                                return account.persistAndFlush()
                                                        .map(savedAccount -> response);
                                            }
                                            return Uni.createFrom().item(response);
                                        });
                            });
                });
    }

    /**
     * Retire l'indicateur pays du numéro de téléphone
     */
    private String formatPhoneNumber(String phoneNumber) {
        // Retire les 3 premiers caractères (225 pour la Côte d'Ivoire)
        return phoneNumber.length() > 3 ? phoneNumber.substring(3) : phoneNumber;
    }
}
