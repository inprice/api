package io.inprice.scrapper.api.helpers;

import java.util.HashSet;
import java.util.Set;

import io.inprice.scrapper.api.app.user.UserRole;
import io.javalin.core.security.Role;

public class AccessRoles {

   private static Set<Role> ROLES_OF_ANYONE;
   private static Set<Role> ROLES_OF_EDITOR;
   private static Set<Role> ROLES_OF_ADMIN;

   public static Set<Role> ADMIN_ONLY() {
      if (ROLES_OF_ADMIN == null) {
         ROLES_OF_ADMIN = new HashSet<>(1);
         ROLES_OF_ADMIN.add(UserRole.ADMIN);
      }
      return ROLES_OF_ADMIN;
   }

   public static Set<Role> EDITOR() {
      if (ROLES_OF_EDITOR == null) {
         ROLES_OF_EDITOR = new HashSet<>(2);
         ROLES_OF_EDITOR.add(UserRole.ADMIN);
         ROLES_OF_EDITOR.add(UserRole.EDITOR);
      }
      return ROLES_OF_EDITOR;
   }

   public static Set<Role> ANYONE() {
      if (ROLES_OF_ANYONE == null) {
         ROLES_OF_ANYONE = new HashSet<>(3);
         ROLES_OF_ANYONE.add(UserRole.ADMIN);
         ROLES_OF_ANYONE.add(UserRole.EDITOR);
         ROLES_OF_ANYONE.add(UserRole.VIEWER);
      }
      return ROLES_OF_ANYONE;
   }

}