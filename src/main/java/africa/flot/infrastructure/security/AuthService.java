package africa.flot.infrastructure.security;

import africa.flot.application.ports.OldPasswordRepository;
import africa.flot.domain.model.Account;
import africa.flot.domain.model.Lead;
import africa.flot.domain.model.OldPassword;
import africa.flot.infrastructure.repository.AccountRepository;
import africa.flot.infrastructure.repository.SessionRepository;
import africa.flot.infrastructure.repository.TokenBlacklistRepository;
import africa.flot.infrastructure.repository.impl.UserRepositoryImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.redis.client.reactive.ReactiveRedisClient;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.*;

@ApplicationScoped
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class);

    @Inject
    AccountRepository accountRepository;

    @Inject
    UserRepositoryImpl userRepository;

    @Inject
    TokenBlacklistRepository tokenBlacklistRepository;

    @Inject
    OldPasswordRepository oldPasswordRepository;

    @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "flot")
    String issuer;

    @ConfigProperty(name = "jwt.duration", defaultValue = "PT1H")
    Duration jwtDuration;

    @Inject
    SessionRepository sessionRepository;

    @ConfigProperty(name = "session.lifetime", defaultValue = "120")
    int sessionLifetime;

    @ConfigProperty(name = "auth.admin.key")
    String expectedApiKey;

    @Inject
    ReactiveRedisClient redisClient;

    @Inject
    MinioClient minioClient;

    @Inject
    PgPool client;

    private static final String USER_CACHE_KEY = "user:info:";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public Uni<Map<String, Object>> getUserInfo(String identifier, String role) {
        String cacheKey = USER_CACHE_KEY + role + ":" + identifier;

        return redisClient.get(cacheKey)
                .onItem().transformToUni(cached -> {
                    if (cached != null) {
                        try {
                            LOG.info("Cache hit for user: " + identifier);
                            @SuppressWarnings("unchecked")
                            Map<String, Object> result = objectMapper.readValue(cached.toString(), Map.class);
                            return Uni.createFrom().item(result);
                        } catch (Exception e) {
                            LOG.error("Error deserializing cached user info", e);
                        }
                    }
                    return fetchAndCacheUserInfo(identifier, role, cacheKey);
                });
    }


    private Uni<Map<String, Object>> fetchAndCacheUserInfo(String identifier, String role, String cacheKey) {
        return getAuthenticatedUser(identifier, role)
                .onItem().transformToUni(user -> {
                    if (user == null) {
                        return Uni.createFrom().nullItem();
                    }

                    if (role.equals("ADMIN")) {
                        Map<String, Object> adminData = Map.of("user", user, "photoUrl", null);
                        return cacheAndReturn(adminData, cacheKey);
                    }

                    Lead lead = (Lead) user;
                    return client.preparedQuery(
                                    "SELECT a.name FROM attachments a " +
                                            "JOIN attachment_lists al ON a.attachment_lists_id = al.id " +
                                            "WHERE al.slug = 'PHOTO' AND a.key_form_id = $1")
                            .execute(Tuple.of(lead.getKeyForm()))
                            .onItem().transformToUni(rows -> {
                                if (!rows.iterator().hasNext()) {
                                    Map<String, Object> noPhotoData = Map.of("user", user, "photoUrl", null);
                                    return cacheAndReturn(noPhotoData, cacheKey);
                                }

                                try {
                                    String fileName = rows.iterator().next().getString("name");
                                    String photoUrl = minioClient.getPresignedObjectUrl(
                                            GetPresignedObjectUrlArgs.builder()
                                                    .bucket("flotkyb")
                                                    .object(fileName)
                                                    .method(Method.GET)
                                                    .expiry((int) jwtDuration.getSeconds())
                                                    .build()
                                    );
                                    Map<String, Object> photoData = Map.of("user", user, "photoUrl", photoUrl);
                                    return cacheAndReturn(photoData, cacheKey);
                                } catch (Exception e) {
                                    LOG.error("Error generating presigned URL", e);
                                    Map<String, Object> errorData = Map.of("user", user, "photoUrl", null);
                                    return cacheAndReturn(errorData, cacheKey);
                                }
                            });
                });
    }

    private Uni<Map<String, Object>> cacheAndReturn(Map<String, Object> data, String cacheKey) {
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            return redisClient.setex(cacheKey, String.valueOf(jwtDuration.getSeconds()), jsonData)
                    .onItem().transform(result -> {
                        LOG.info("User info cached successfully");
                        return data;
                    })
                    .onFailure().recoverWithItem(e -> {
                        LOG.error("Failed to cache user info", e);
                        return data;
                    });
        } catch (Exception e) {
            LOG.error("Error serializing user info", e);
            return Uni.createFrom().item(data);
        }
    }

    public Uni<Void> invalidateUserCache(String identifier, String role) {
        String cacheKey = USER_CACHE_KEY + role + ":" + identifier;
        return redisClient.del(List.of(cacheKey))
                .onItem().transform(result -> null);
    }

    public Uni<Boolean> authenticateSubscriber(String username, String password) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            LOG.warn("Tentative d'authentification abonné avec des identifiants vides ou nuls");
            return Uni.createFrom().item(false);
        }

        return accountRepository.findByUsername(username)
                .onItem().transform(account -> {
                    if (account != null && account.isActive) {
                        boolean matches = BcryptUtil.matches(password, account.passwordHash);
                        LOG.info("Tentative d'authentif1ication abonné pour " + username + ": " + (matches ? "réussie" : "échouée"));
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


    public Uni<Boolean> invalidateToken(String token) {
        // Définir la durée de validité restante du jeton en secondes (par exemple, 3600 secondes pour 1 heure)
        long expirationTimeInSeconds = jwtDuration.getSeconds();
        return tokenBlacklistRepository.add(token, expirationTimeInSeconds);
    }


    public Uni<Object> findAdminByEmail(String email) {
        // Implémenter la logique pour trouver un admin par email
        return userRepository.findByEmail(email)
                .onItem().transform(user -> user);
    }


    @WithTransaction
    public Uni<Response> changePassword(String username, String oldPassword, String newPassword) {
        if (username == null || oldPassword == null || newPassword == null || oldPassword.equals(newPassword)) {
            LOG.warn("changePassword: Données invalides fournies");
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST).entity("Données invalides ou mot de passe identique à l'ancien").build());
        }

        return accountRepository.findByUsername(username)
                .onItem().transformToUni(account -> {
                    if (account != null && BcryptUtil.matches(oldPassword, account.passwordHash)) {
                        String newHash = BcryptUtil.bcryptHash(newPassword);

                        return oldPasswordRepository.isPasswordUsed(account.id, newHash)
                                .onItem().transformToUni(isUsed -> {
                                    if (isUsed) {
                                        LOG.warn("changePassword: Nouveau mot de passe déjà utilisé pour " + username);
                                        return Uni.createFrom().item(Response.status(Response.Status.CONFLICT).entity("Nouveau mot de passe déjà utilisé").build());
                                    }
                                    account.passwordHash = newHash;
                                    return accountRepository.updatePassword(account.getLead().getId(), account.passwordHash)
                                            .onItem().transformToUni(updated -> {
                                                LOG.info("Mot de passe changé avec succès pour : " + username);
                                                OldPassword oldPasswordEntity = new OldPassword();
                                                oldPasswordEntity.account = account;
                                                oldPasswordEntity.passwordHash = newHash;
                                                return oldPasswordRepository.saveOldPassword(oldPasswordEntity)
                                                        .onItem().transformToUni(saved ->
                                                                invalidateUserCache(username, "SUBSCRIBER")
                                                                        .onItem().transform(v ->
                                                                                Response.ok("Mot de passe mis à jour avec succès").build()
                                                                        )
                                                        );
                                            });
                                });
                    } else {
                        LOG.warn("changePassword: Ancien mot de passe incorrect pour " + username);
                        return Uni.createFrom().item(Response.status(Response.Status.UNAUTHORIZED).entity("Ancien mot de passe incorrect").build());
                    }
                })
                .onFailure().recoverWithItem(e -> {
                    LOG.error("Erreur lors de la modification du mot de passe", e);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Erreur interne").build();
                });
    }

    public Uni<Map<String, Object>> authenticateAdminWithApiKeyAndSession(String email, String apiKey, String sessionId) {
        if (email == null || apiKey == null || sessionId == null) {
            LOG.warn("authenticateAdminWithApiKeyAndSession: email, API Key, or session ID is null");
            return Uni.createFrom().nullItem(); // Correction ici pour éviter l'erreur `null`
        }

        // Vérification de l'API Key
        if (!apiKey.equals(expectedApiKey)) {
            LOG.warn("authenticateAdminWithApiKeyAndSession: Invalid API Key");
            return Uni.createFrom().nullItem(); // Correction ici aussi
        }

        // Vérification de l'email
        if (!email.endsWith("@flot.africa")) {
            LOG.warn("authenticateAdminWithApiKeyAndSession: Unauthorized email domain: " + email);
            return Uni.createFrom().nullItem(); // Correction ici aussi
        }

        // Vérification de la session Laravel
        return isLaravelSessionActive(sessionId)
                .onItem().transformToUni(isSessionActive -> {
                    if (!isSessionActive) {
                        LOG.warn("authenticateAdminWithApiKeyAndSession: Inactive or invalid Laravel session for session ID " + sessionId);
                        return Uni.createFrom().nullItem(); // Correction ici aussi
                    }

                    // Générer le JWT si toutes les vérifications passent
                    return generateAdminJWT(email)
                            .onItem().transform(jwtData -> {
                                LOG.info("authenticateAdminWithApiKeyAndSession: JWT generated for admin with email: " + email);
                                return jwtData;
                            });
                });
    }

    public Uni<Boolean> isLaravelSessionActive(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            LOG.warn("isLaravelSessionActive: Session ID is missing");
            return Uni.createFrom().item(false);
        }

        int sessionTimeoutInSeconds = sessionLifetime * 60; // Convertir en secondes

        return sessionRepository.findBySessionId(sessionId)
                .onItem().transform(session -> session != null && session.isActive(sessionTimeoutInSeconds));
    }


}
