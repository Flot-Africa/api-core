package africa.flot.infrastructure.security;

import africa.flot.domain.model.Account;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SecurityService {
    private static final Logger LOG = Logger.getLogger(SecurityService.class);
    private static final Logger AUDIT_LOG = Logger.getLogger("AUDIT");
    private static final Logger errorf_LOG = Logger.getLogger("errorf");

    @Inject
    SecurityIdentity identity;

    @WithSession
    public Uni<Void> validateLeadAccess(String leadId) {
        // If not a SUBSCRIBER, no need to validate
        if (!identity.hasRole("SUBSCRIBER")) {
            AUDIT_LOG.debugf("Skipping validation for non-SUBSCRIBER user: %s",
                    identity.getPrincipal().getName());
            return Uni.createFrom().voidItem();
        }

        String username = identity.getPrincipal().getName();
        AUDIT_LOG.debugf("Validating lead access for SUBSCRIBER %s to Lead %s", username, leadId);

        return Account.<Account>find("username = ?1", username)
                .firstResult()
                .onItem().invoke(Unchecked.consumer(account -> {
                    if (account == null) {
                        AUDIT_LOG.errorf("Account not found for username: %s", username);
                        throw new NotFoundException("Resource not found");
                    }

                    if (!account.getLead().getId().toString().equals(leadId)) {
                        AUDIT_LOG.errorf("Unauthorized access attempt - User: %s tried to access Lead: %s",
                                username, leadId);
                        // Return 404 instead of 403 to not reveal resource existence
                        throw new NotFoundException("Resource not found");
                    }

                    AUDIT_LOG.infof("Access validated for SUBSCRIBER %s to Lead %s", username, leadId);
                }))
                .replaceWithVoid();
    }
}