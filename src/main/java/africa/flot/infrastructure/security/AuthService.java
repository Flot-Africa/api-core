package africa.flot.infrastructure.security;

import africa.flot.domain.model.Account;
import africa.flot.domain.model.Lead;
import africa.flot.infrastructure.repository.AccountRepository;
import africa.flot.infrastructure.repository.impl.UserRepositoryImpl;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class);

    @Inject
    AccountRepository accountRepository;

    @Inject
    UserRepositoryImpl userRepository;

    @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "flot")
    String issuer;

    @ConfigProperty(name = "jwt.duration", defaultValue = "PT1H")
    Duration jwtDuration;

    public Uni<Boolean> authenticateSubscriber(String username, String password) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            LOG.warn("Tentative d'authentification abonné avec des identifiants vides ou nuls");
            return Uni.createFrom().item(false);
        }

        return accountRepository.findByUsername(username)
                .onItem().transform(account -> {
                    if (account != null && account.isActive) {
                        boolean matches = BcryptUtil.matches(password, account.passwordHash);
                        LOG.info("Tentative d'authentification abonné pour " + username + ": " + (matches ? "réussie" : "échouée"));
                        return matches;
                    }
                    LOG.warn("Aucun compte actif trouvé pour l'utilisateur : " + username);
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

    public Uni<Map<String, Object>> generateSubscriberJWT(String username) {
        if (username == null) {
            LOG.warn("generateSubscriberJWT: Invalid username received, username is null");
            return Uni.createFrom().failure(new IllegalArgumentException("Invalid input: username must not be null"));
        }
        return accountRepository.findByUsername(username)
                .onItem().transform(account -> {
                    if (account != null && account.isActive) {
                        String token = Jwt.issuer(issuer)
                                .subject(account.username)
                                .groups(Set.of("SUBSCRIBER"))
                                .claim("username", account.username)
                                .claim("subscriberId", account.lead.getId().toString())
                                .expiresIn(jwtDuration)
                                .sign();
                        LOG.info("JWT généré pour l'abonné " + username);
                        Map<String, Object> jwtData = new HashMap<>();
                        jwtData.put("token", token);
                        jwtData.put("expiresIn", jwtDuration.getSeconds());
                        return jwtData;
                    }
                    LOG.warn("Impossible de générer JWT pour un compte non trouvé ou inactif : " + username);
                    return null;
                })
                .onFailure().invoke(e -> LOG.error("Erreur lors de la génération du JWT abonné", e));
    }


    public Uni<Map<String, Object>> generateAdminJWT(String email) {
        if (email == null) {
            LOG.warn("generateAdminJWT: Invalid email received, email is null");
            return Uni.createFrom().failure(new IllegalArgumentException("Invalid input: email must not be null"));
        }
        return userRepository.findByEmail(email)
                .onItem().transform(user -> {
                    if (user != null) {
                        String token = Jwt.issuer(issuer)
                                .subject(user.email)
                                .groups(Set.of("ADMIN"))
                                .claim("email", user.email)
                                .expiresIn(jwtDuration)
                                .sign();
                        LOG.info("JWT généré pour l'admin " + email);
                        Map<String, Object> jwtData = new HashMap<>();
                        jwtData.put("token", token);
                        jwtData.put("expiresIn", jwtDuration.getSeconds());
                        return jwtData;
                    }
                    LOG.warn("Impossible de générer JWT pour un admin non trouvé : " + email);
                    return null;
                })
                .onFailure().invoke(e -> LOG.error("Erreur lors de la génération du JWT admin", e));
    }

    public Uni<String> hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            LOG.warn("Tentative de hachage d'un mot de passe vide ou nul");
            return Uni.createFrom().failure(new IllegalArgumentException("Le mot de passe ne peut pas être vide ou nul"));
        }

        return Uni.createFrom().item(() -> {
            String hashedPassword = BcryptUtil.bcryptHash(password);
            LOG.info("Mot de passe haché avec succès");
            return hashedPassword;
        }).onFailure().invoke(e -> LOG.error("Erreur lors du hachage du mot de passe", e));
    }

    public Uni<Void> createAccount(UUID subscriberId, String username, String password) {
        if (subscriberId == null || username == null || username.isEmpty() || password == null || password.isEmpty()) {
            LOG.warn("Tentative de création de compte avec des données invalides");
            return Uni.createFrom().failure(new IllegalArgumentException("Données de création de compte invalides"));
        }

        return hashPassword(password)
                .onItem().transformToUni(hashedPassword -> {
                    Account account = new Account();
                    account.lead = new Lead();
                    account.lead.setId(subscriberId);
                    account.username = username;
                    account.passwordHash = hashedPassword;
                    account.isActive = true;
                    return accountRepository.persist(account);
                })
                .onItem().invoke(() -> LOG.info("Nouveau compte créé pour l'abonné : " + username))
                .replaceWithVoid();
    }

    public Uni<Object> getAuthenticatedUser(String identifier, String role) {
        if (role.equals("SUBSCRIBER")) {
            return findSubscriberByUsername(identifier)
                    .onItem().transform(subscriber -> {
                        if (subscriber != null) {
                            LOG.info("getAuthenticatedUser: Abonné trouvé pour le nom d'utilisateur : " + identifier);
                            return subscriber;
                        } else {
                            LOG.warn("getAuthenticatedUser: Aucun abonné trouvé pour le nom d'utilisateur : " + identifier);
                            return null;
                        }
                    });
        } else if (role.equals("ADMIN")) {
            return findAdminByEmail(identifier)
                    .onItem().transform(admin -> {
                        if (admin != null) {
                            LOG.info("getAuthenticatedUser: Admin trouvé pour l'email : " + identifier);
                            return admin;
                        } else {
                            LOG.warn("getAuthenticatedUser: Aucun admin trouvé pour l'email : " + identifier);
                            return null;
                        }
                    });
        } else {
            LOG.warn("getAuthenticatedUser: Rôle inconnu");
            return Uni.createFrom().failure(new IllegalArgumentException("Rôle inconnu"));
        }
    }

    public Uni<Lead> findSubscriberByUsername(String username) {
        return accountRepository.findByUsername(username)
                .onItem().transform(account -> {
                    if (account != null && account.lead != null) {
                        LOG.info("findSubscriberByUsername: Lead trouvé pour le nom d'utilisateur : " + username);
                        return account.lead;
                    } else {
                        LOG.warn("findSubscriberByUsername: Aucun Lead trouvé pour le nom d'utilisateur : " + username);
                        return null;
                    }
                });
    }


    public Uni<Object> findAdminByEmail(String email) {
        // Implémenter la logique pour trouver un admin par email
        return userRepository.findByEmail(email)
                .onItem().transform(user -> user);
    }
}
