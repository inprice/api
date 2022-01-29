package io.inprice.api.session;

import io.inprice.api.session.info.ForRedis;
import io.inprice.common.meta.UserRole;
import io.inprice.common.meta.WorkspaceStatus;
import io.inprice.common.models.User;

/**
 * Manages two type of users; 
 * 	a) Super user, 
 * 	b) Normal users
 * Super user has time limited token while normals have unlimeted ones!
 * 
 * @author mdpinar
 *
 */
public class CurrentUser {

  private static ThreadLocal<ThreadVariables> THREAD_VARIABLES = new ThreadLocal<ThreadVariables>() {
    @Override
    protected ThreadVariables initialValue() {
      return new ThreadVariables();
    }
  };
  
  //for super user
  static void set(User superUser) {
    THREAD_VARIABLES.get().set(superUser);
  }
  
  //for normal users
  static void set(ForRedis forRedis, int sessionNo) {
    THREAD_VARIABLES.get().set(forRedis, sessionNo);
  }

  public static int getSessionNo() {
    return THREAD_VARIABLES.get().getSessionNo();
  }

  public static boolean hasSession() {
    return THREAD_VARIABLES.get() != null && THREAD_VARIABLES.get().getSession() != null;
  }

  public static String getSessionHash() {
    return THREAD_VARIABLES.get().getSession().getHash();
  }

  public static Long getUserId() {
  	if (THREAD_VARIABLES.get().getSuperUser() != null) {
  		return THREAD_VARIABLES.get().getSuperUser().getId();
  	} else {
  		return THREAD_VARIABLES.get().getSession().getUserId();
  	}
  }

  public static String getEmail() {
  	if (THREAD_VARIABLES.get().getSuperUser() != null) {
  		return THREAD_VARIABLES.get().getSuperUser().getEmail();
  	} else {
  		return THREAD_VARIABLES.get().getSession().getEmail();
  	}
  }

  public static String getFullName() {
  	if (THREAD_VARIABLES.get().getSuperUser() != null) {
  		return THREAD_VARIABLES.get().getSuperUser().getFullName();
  	} else {
  		return THREAD_VARIABLES.get().getSession().getFullName();
  	}
  }

  public static String getUserTimezone() {
  	if (THREAD_VARIABLES.get().getSuperUser() != null) {
  		return THREAD_VARIABLES.get().getSuperUser().getTimezone();
  	} else {
  		return THREAD_VARIABLES.get().getSession().getTimezone();
  	}
  }

  public static Long getWorkspaceId() {
  	if (THREAD_VARIABLES.get().getSuperUser() != null) {
  		return THREAD_VARIABLES.get().getWorkspaceId();
  	} else {
  		return THREAD_VARIABLES.get().getSession().getWorkspaceId();
  	}
  }

	public static String getWorkspaceName() {
  	if (THREAD_VARIABLES.get().getSuperUser() != null) {
  		return "inprice.io";
  	} else {
  		return THREAD_VARIABLES.get().getSession().getWorkspace();
  	}
  }

	public static WorkspaceStatus getWorkspaceStatus() {
  	if (THREAD_VARIABLES.get().getSuperUser() != null) {
  		return THREAD_VARIABLES.get().getWorkspaceStatus();
  	} else {
  		return THREAD_VARIABLES.get().getSession().getWorkspaceStatus();
  	}
  }

  public static UserRole getRole() {
  	if (THREAD_VARIABLES.get().getSuperUser() != null) {
  		return UserRole.SUPER;
  	} else {
  		return THREAD_VARIABLES.get().getSession().getRole();
  	}
  }

  public static void cleanup() {
    THREAD_VARIABLES.remove();
  }

}
