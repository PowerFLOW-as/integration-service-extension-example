package cz.powerflow.integration.dto.pwfservice;

import lombok.Data;

/**
 * Object that is returned after PWF Token verification.
 */
@Data
public class PwfUserVerifyApi {

    private PwfUser user;
    private Boolean valid;

}
