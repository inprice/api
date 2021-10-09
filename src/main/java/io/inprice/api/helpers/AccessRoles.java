package io.inprice.api.helpers;

import java.util.HashSet;
import java.util.Set;

import io.inprice.api.meta.ShadowRoles;
import io.javalin.core.security.Role;

public class AccessRoles {

	//SUPER role must not have editing ability!
	private static Set<Role> SUPER_ROLES = new HashSet<>(1);
	private static Set<Role> ADMIN_ROLES = new HashSet<>(2);
	private static Set<Role> EDITOR_ROLES = new HashSet<>(3);
	private static Set<Role> ANYONE_ROLES = new HashSet<>(4);
	private static Set<Role> ANYONE_EXCEPT_SUPER_ROLES = new HashSet<>(3);
	private static Set<Role> ANYONE_PLUS_SUPER_WITH_WORKSPACE = new HashSet<>(4);
	private static Set<Role> ADMIN_OR_SUPER_ROLES = new HashSet<>(2);
	
	static {
		SUPER_ROLES.add(ShadowRoles.SUPER);

		ADMIN_ROLES.add(ShadowRoles.ADMIN);

		EDITOR_ROLES.addAll(ADMIN_ROLES);
		EDITOR_ROLES.add(ShadowRoles.EDITOR);

		ANYONE_EXCEPT_SUPER_ROLES.addAll(EDITOR_ROLES);
		ANYONE_EXCEPT_SUPER_ROLES.add(ShadowRoles.VIEWER);
		
		ANYONE_ROLES.add(ShadowRoles.SUPER);
		ANYONE_ROLES.addAll(ANYONE_EXCEPT_SUPER_ROLES);

		ANYONE_PLUS_SUPER_WITH_WORKSPACE.addAll(ANYONE_EXCEPT_SUPER_ROLES);
		ANYONE_PLUS_SUPER_WITH_WORKSPACE.add(ShadowRoles.SUPER_WITH_WORKSPACE);

		ADMIN_OR_SUPER_ROLES.add(ShadowRoles.SUPER);
		ADMIN_OR_SUPER_ROLES.add(ShadowRoles.ADMIN);
	}
	
	public static Set<Role> SUPER_ONLY() {
		return SUPER_ROLES;
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

	public static Set<Role> ANYONE_EXCEPT_SUPER() {
		return ANYONE_EXCEPT_SUPER_ROLES;
	}

	public static Set<Role> ANYONE_PLUS_SUPER_WITH_WORKSPACE() {
		return ANYONE_PLUS_SUPER_WITH_WORKSPACE;
	}
	
	public static Set<Role> ADMIN_OR_SUPER() {
		return ADMIN_OR_SUPER_ROLES;
	}

}