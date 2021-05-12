package io.inprice.api.helpers;

import java.util.HashSet;
import java.util.Set;

import io.inprice.api.meta.ShadowRoles;
import io.javalin.core.security.Role;

public class AccessRoles {

	private static Set<Role> SUPER_ROLES = new HashSet<>(1);
	private static Set<Role> ADMIN_ROLES = new HashSet<>(2);
	private static Set<Role> EDITOR_ROLES = new HashSet<>(3);
	private static Set<Role> ANYONE_ROLES = new HashSet<>(4);
	
	static {
		SUPER_ROLES.add(ShadowRoles.SUPER);

		ADMIN_ROLES.addAll(SUPER_ROLES);
		ADMIN_ROLES.add(ShadowRoles.ADMIN);

		EDITOR_ROLES.addAll(ADMIN_ROLES);
		EDITOR_ROLES.add(ShadowRoles.EDITOR);

		ANYONE_ROLES.addAll(EDITOR_ROLES);
		ANYONE_ROLES.add(ShadowRoles.VIEWER);
	}
	
	public static Set<Role> SUPER_ONLY() {
		return ADMIN_ROLES;
	}

	public static Set<Role> ADMIN() {
		return ADMIN_ROLES;
	}

	public static Set<Role> EDITOR() {
		return EDITOR_ROLES;
	}

	public static Set<Role> ANYONE() {
		return ANYONE_ROLES;
	}

}