package africa.flot.infrastructure.security;

import africa.flot.domain.model.Subscriber;
import africa.flot.domain.repository.SubscriberRepository;
import africa.flot.infrastructure.persistence.UserRepositoryImpl;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Set;

@ApplicationScoped
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class);

    @Inject
    SubscriberRepository subscriberRepository;

    @Inject
    UserRepositoryImpl userRepository;

    @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "flot")
    String issuer;

    @ConfigProperty(name = "jwt.duration", defaultValue = "PT1H")
    Duration jwtDuration;

    public Uni<Boolean> authenticateSubscriber(String phone, String password) {
        if (phone == null || phone.isEmpty() || password == null || password.isEmpty()) {
            LOG.warn("Tentative d'authentification abonné avec des identifiants vides ou nuls");
            return Uni.createFrom().item(false);
        }

        return subscriberRepository.findByPhone(phone)
                .onItem().transform(subscriber -> {
                    if (subscriber != null) {
                        boolean matches = BcryptUtil.matches(password, subscriber.password);
                        LOG.info("Tentative d'authentification abonné pour " + phone + ": " + (matches ? "réussie" : "échouée"));
                        return matches;
                    }
                    LOG.warn("Aucun abonné trouvé pour le numéro : " + phone);
                    return false;
                })
                .onFailure().invoke(e -> LOG.error("Erreur lors de l'authentification abonné", e));
    }

    public Uni<Boolean> authenticateAdmin(String email, String password) {
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            LOG.warn("Tentative d'authentification admin avec des identifiants vides ou nuls");
            return Uni.createFrom().item(false);
        }

        if (!email.endsWith("@flot.africa")) {
            LOG.warn("Tentative d'authentification admin avec un email non autorisé : " + email);
            return Uni.createFrom().item(false);
        }

        return userRepository.findByEmail(email)
                .onItem().transform(user -> {
                    if (user != null) {
                        boolean matches = BcryptUtil.matches(password, user.password);
                        LOG.info("Tentative d'authentification admin pour " + email + ": " + (matches ? "réussie" : "échouée"));
                        return matches;
                    }
                    LOG.warn("Aucun admin trouvé pour l'email : " + email);
                    return false;
                })
                .onFailure().invoke(e -> LOG.error("Erreur lors de l'authentification admin", e));
    }

    public Uni<String> generateSubscriberJWT(String phone) {
        return subscriberRepository.findByPhone(phone)
                .onItem().transform(subscriber -> {
                    if (subscriber != null) {
                        String token = Jwt.issuer(issuer)
                                .subject(subscriber.id.toString())
                                .groups(Set.of("SUBSCRIBER"))
                                .claim("phone", subscriber.phone)
                                .claim("email", subscriber.email)
                                .expiresIn(jwtDuration)
                                .sign();
                        LOG.info("JWT généré pour l'abonné " + phone);
                        return token;
                    }
                    LOG.warn("Impossible de générer JWT pour un abonné non trouvé : " + phone);
                    return null;
                })
                .onFailure().invoke(e -> LOG.error("Erreur lors de la génération du JWT abonné", e));
    }

    public Uni<String> generateAdminJWT(String email) {
        return userRepository.findByEmail(email)
                .onItem().transform(user -> {
                    if (user != null) {
                        String token = Jwt.issuer(issuer)
                                .subject(user.id.toString())
                                .groups(Set.of("ADMIN"))
                                .claim("email", user.email)
                                .expiresIn(jwtDuration)
                                .sign();
                        LOG.info("JWT généré pour l'admin " + email);
                        return token;
                    }
                    LOG.warn("Impossible de générer JWT pour un admin non trouvé : " + email);
                    return null;
                })
                .onFailure().invoke(e -> LOG.error("Erreur lors de la génération du JWT admin", e));
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
