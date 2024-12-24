package africa.flot.application.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisbursementDataDTO {
    private String expectedDisbursementDate;
    private Long principal;
}