package io.inprice.scrapper.api.helpers;

import java.util.HashSet;
import java.util.Set;

import io.inprice.scrapper.api.meta.ShadowRoles;
import io.javalin.core.security.Role;

public class AccessRoles {

   private static Set<Role> ROLES_OF_ANYONE;
   private static Set<Role> ROLES_OF_EDITOR;
   private static Set<Role> ROLES_OF_ADMIN;

   public static Set<Role> ADMIN_ONLY() {
      if (ROLES_OF_ADMIN == null) {
         ROLES_OF_ADMIN = new HashSet<>(1);
         ROLES_OF_ADMIN.add(ShadowRoles.ADMIN);
      }
      return ROLES_OF_ADMIN;
   }

   public static Set<Role> EDITOR() {
      if (ROLES_OF_EDITOR == null) {
         ROLES_OF_EDITOR = new HashSet<>(2);
         ROLES_OF_EDITOR.add(ShadowRoles.ADMIN);
         ROLES_OF_EDITOR.add(ShadowRoles.EDITOR);
      }
      return ROLES_OF_EDITOR;
   }

   public static Set<Role> ANYONE() {
      if (ROLES_OF_ANYONE == null) {
         ROLES_OF_ANYONE = new HashSet<>(3);
         ROLES_OF_ANYONE.add(ShadowRoles.ADMIN);
         ROLES_OF_ANYONE.add(ShadowRoles.EDITOR);
         ROLES_OF_ANYONE.add(ShadowRoles.VIEWER);
      }
      return ROLES_OF_ANYONE;
   }

}