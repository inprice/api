package io.inprice.api.meta;

/**
 * These enums are used in checking if a workspace has a limit or allowance for a specific case
 * 
 * @author mdpinar
 */
public enum WSPermission {

	PRODUCT_LIMIT,
	ALARM_LIMIT,
	USER_LIMIT,
	
	API_ALLOWED,
	INTEGRATIONS_ALLOWED,
	SEARCH_INSERT_ALLOWED;

}
