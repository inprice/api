package io.inprice.api.meta;

import io.javalin.core.security.Role;

public enum ShadowRoles implements Role {

	SUPER,
	SUPER_WITH_WORKSPACE,

	ADMIN,
  EDITOR,
  VIEWER;
 
}