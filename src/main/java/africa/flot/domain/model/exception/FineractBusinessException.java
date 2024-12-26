package africa.flot.domain.model.exception;

import africa.flot.application.dto.response.FineractErrorResponse;

public class FineractBusinessException extends BusinessException {
    private final FineractErrorResponse fineractErrorResponse;

    public FineractBusinessException(FineractErrorResponse fineractErrorResponse) {
        super(fineractErrorResponse.getDefaultUserMessage());
        this.fineractErrorResponse = fineractErrorResponse;
    }

    public FineractErrorResponse getFineractErrorResponse() {
        return fineractErrorResponse;
    }
}
