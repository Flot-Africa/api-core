package africa.flot.application.dto.command;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
public class DocumentRequest {
    private String bucketName;
    private String frontImageName;
    private String backImageName;
    private UUID leadId;
}

