package africa.flot.application.dto.response;

import lombok.Data;

@Data
public class ClientLoanAccountResponse {
    private Long id;
    private Long clientId;
    private Long loanProductId;
    private String status;
    private String errorMessage;
}
