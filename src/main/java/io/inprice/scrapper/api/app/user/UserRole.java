package io.inprice.scrapper.api.app.user;

import io.javalin.core.security.Role;

public enum UserRole implements Role {

   ADMIN,
   EDITOR,
   VIEWER;

}
