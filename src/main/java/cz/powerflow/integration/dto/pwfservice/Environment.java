package cz.powerflow.integration.dto.pwfservice;

import java.io.Serializable;

/**
 * Enum that is a part of a response from PWF Token verification.
 */
public enum Environment implements Serializable {
	DEVELOPMENT, TEST, STAGE, PRODUCTION
}
