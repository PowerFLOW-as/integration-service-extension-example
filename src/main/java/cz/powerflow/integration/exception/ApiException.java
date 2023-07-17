package cz.powerflow.integration.exception;

public abstract class ApiException extends RuntimeException {

    private final static String DETAIL_SEPARATOR = " ";

    private String publicMessage;

    public ApiException() {
    }

    public ApiException(String publicMessage) {
        super(publicMessage);
        this.publicMessage = publicMessage;
    }

    public ApiException(Throwable cause) {
        super(cause);
    }

    public ApiException(String publicMessage, Throwable cause) {
        super(publicMessage, cause);
        this.publicMessage = publicMessage;
    }

    public ApiException(String publicMessage, String detail) {
        super(publicMessage + DETAIL_SEPARATOR + detail);
        this.publicMessage = publicMessage;
    }

    public ApiException(String publicMessage, String detail, Throwable cause) {
        super(publicMessage + DETAIL_SEPARATOR + detail, cause);
        this.publicMessage = publicMessage;
    }

    public String getPublicMessage() {
        return publicMessage;
    }
}
