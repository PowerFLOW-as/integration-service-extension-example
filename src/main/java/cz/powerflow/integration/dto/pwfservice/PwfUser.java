package cz.powerflow.integration.dto.pwfservice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * Sub-object that is returned after PWF Token verification.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PwfUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean local;
    private String commonName;
    private String fullName;
    private String givenName;
    private String surname;
    private String uid;
    private Map<String, String> additionalAttributes;
    private String email;
    private Boolean mustChangePassword;
    private String locale;
    private Environment environment;
    private Boolean notificationUpdatedByUser;
    private Boolean archived;
    private Boolean active;
    private String lastLogin;

}
