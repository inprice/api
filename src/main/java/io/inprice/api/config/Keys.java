package io.inprice.api.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Keys {

	@JsonProperty("superUser")
	public String SUPER_USER;

	@JsonProperty("user")
	public String USER;

	@JsonProperty("encryption")
	public String ENCRYPTION;

	@JsonProperty("geoLocation")
	public String GEO_LOCATION;
	
}
