package africa.flot.domain.model;

import africa.flot.application.dto.query.DanayaVerificationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "danaya_verification_results")
@Data
public class DanayaVerificationResults extends PanacheEntityBase {
    @Id
    private UUID id;

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Column(name = "status")
    private String status;

    @Column(name = "response_details", columnDefinition = "TEXT")
    private String responseDetails;

    @Column(name = "created_at")
    private String createdAt;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Méthode pour initialiser ou mettre à jour l'entité à partir d'un DanayaVerificationResult
    public void updateFromVerificationResult(DanayaVerificationResult result) {
        this.id = result.getId();
        this.status = result.getStatus();
        this.createdAt = result.getCreatedAt();
        this.responseDetails = convertResultToJson(result);
    }

    private String convertResultToJson(DanayaVerificationResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{}"; // Retourner une valeur par défaut en cas d'erreur
        }
    }
}