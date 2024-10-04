package africa.flot.infrastructure.security;

import africa.flot.domain.model.Subscriber;
import africa.flot.domain.repository.SubscriberRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class);

    @Inject
    SubscriberRepository subscriberRepository;

    @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "flot")
    String issuer;

    @ConfigProperty(name = "jwt.duration", defaultValue = "PT1H")
    Duration jwtDuration;

    public Uni<Boolean> authenticate(String phone, String password) {
        if (phone == null || phone.isEmpty() || password == null || password.isEmpty()) {
            LOG.warn("Tentative d'authentification avec des identifiants vides ou nuls");
            return Uni.createFrom().item(false);
        }

        return subscriberRepository.findByPhone(phone)
                .onItem().transform(subscriber -> {
                    if (subscriber != null) {
                        boolean matches = BcryptUtil.matches(password, subscriber.password);
                        LOG.info("Tentative d'authentification pour " + phone + ": " + (matches ? "réussie" : "échouée"));
                        return matches;
                    }
                    LOG.warn("Aucun abonné trouvé pour le numéro : " + phone);
                    return false;
                })
                .onFailure().invoke(e -> LOG.error("Erreur lors de l'authentification", e));
    }

    public Uni<String> generateJWT(String phone) {
        return subscriberRepository.findByPhone(phone)
                .onItem().transform(subscriber -> {
                    if (subscriber != null) {
                        Set<String> roles = determineRoles(subscriber);
                        String token = Jwt.issuer(issuer)
                                .subject(subscriber.id.toString())
                                .groups(roles)
                                .claim("phone", subscriber.phone)
                                .claim("email", subscriber.email)
                                .expiresIn(jwtDuration)
                                .sign();
                        LOG.info("JWT généré pour " + phone);
                        return token;
                    }
                    LOG.warn("Impossible de générer JWT pour un abonné non trouvé : " + phone);
                    return null;
                })
                .onFailure().invoke(e -> LOG.error("Erreur lors de la génération du JWT", e));
    }


    private Set<String> determineRoles(Subscriber subscriber) {
        Set<String> roles = new HashSet<>();
        roles.add("SUBSCRIBER");

        if (subscriber.email != null && subscriber.email.endsWith("@flot.africa")) {
            roles.add("ADMIN");
            LOG.info("Rôle ADMIN attribué à " + subscriber.email);
        }

        return roles;
    }

    public Uni<Void> registerSubscriber(Subscriber subscriber, String password) {
        if (subscriber == null || password == null || password.isEmpty()) {
            LOG.warn("Tentative d'enregistrement avec des données invalides");
            return Uni.createFrom().failure(new IllegalArgumentException("Données d'enregistrement invalides"));
        }

        subscriber.password = BcryptUtil.bcryptHash(password);
        LOG.info("Enregistrement d'un nouvel abonné : " + subscriber.email);
        return Uni.createFrom().voidItem();
    }
}