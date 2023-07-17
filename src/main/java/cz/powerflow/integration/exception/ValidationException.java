package cz.powerflow.integration.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class ValidationException extends ApiException {

    public ValidationException(String publicMessage) {
        super(publicMessage);
    }

    public ValidationException(String publicMessage, Throwable cause) {
        super(publicMessage, cause);
    }

    public ValidationException(String publicMessage, String detail) {
        super(publicMessage, detail);
    }

    public ValidationException(String publicMessage, String detail, Throwable cause) {
        super(publicMessage, detail, cause);
    }
}
