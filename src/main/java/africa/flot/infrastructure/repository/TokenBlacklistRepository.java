package africa.flot.infrastructure.repository;

import io.quarkus.redis.client.reactive.ReactiveRedisClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class TokenBlacklistRepository {

    private static final Logger LOG = Logger.getLogger(TokenBlacklistRepository.class);

    @Inject
    ReactiveRedisClient redisClient;

    private static final String TOKEN_BLACKLIST_PREFIX = "blacklist:";

    public Uni<Boolean> add(String token, long expirationTimeInSeconds) {
        String redisKey = TOKEN_BLACKLIST_PREFIX + token;
        return redisClient.setex(redisKey, String.valueOf(expirationTimeInSeconds), token)
                .onItem().transform(response -> {
                    LOG.info("Token ajouté à la blacklist avec une expiration de " + expirationTimeInSeconds + " secondes.");
                    return response != null && response.toString().equalsIgnoreCase("OK");
                })
                .onFailure().invoke(e -> LOG.error("Erreur lors de l'ajout du token à la blacklist", e));
    }

    public Uni<Boolean> isTokenBlacklisted(String token) {
        String redisKey = TOKEN_BLACKLIST_PREFIX + token;
        return redisClient.exists(List.of(redisKey))
                .onItem().transform(response -> response.toInteger() > 0);
    }
}