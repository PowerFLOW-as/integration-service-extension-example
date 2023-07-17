package cz.powerflow.integration.dto.pwfservice;

import lombok.Data;

import java.util.List;

/**
 * Sub-object that is returned after PWF Token verification.
 */
@Data
public class PwfUserList {
    private List<PwfUser> users;
}
