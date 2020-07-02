package io.inprice.api.meta;

import io.javalin.core.security.Role;

public enum ShadowRoles implements Role {

  ADMIN,
  EDITOR,
  VIEWER;
 
}