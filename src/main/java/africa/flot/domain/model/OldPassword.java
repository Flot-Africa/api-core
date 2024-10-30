package africa.flot.domain.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
public class OldPassword extends PanacheEntity {

    @ManyToOne
    public Account account; // Lien avec le compte utilisateur
    public String passwordHash;
    public LocalDateTime createdAt = LocalDateTime.now();

    public static boolean existsForAccount(Long accountId, String hash) {
        return find("account.id = ?1 and passwordHash = ?2", accountId, hash).firstResult() != null;
    }
}

