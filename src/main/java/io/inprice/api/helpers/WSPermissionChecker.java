package io.inprice.api.helpers;

import java.util.List;

import org.jdbi.v3.core.Handle;

import io.inprice.api.app.workspace.WorkspaceDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.info.Response;
import io.inprice.api.meta.WSPermission;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.models.Workspace;

public class WSPermissionChecker {

  public static Response checkFor(WSPermission permission, Handle handle) {
    return checkFor(List.of(permission), handle);
  }

  public static Response checkFor(List<WSPermission> permissions, Handle handle) {
    WorkspaceDao workspaceDao = handle.attach(WorkspaceDao.class);
    Workspace workspace = workspaceDao.findById(CurrentUser.getWorkspaceId());
    
    return check(workspace, permissions);
  }

  private static Response check(Workspace workspace, List<WSPermission> permissions) {
  	Response res = Responses.OK;
    
    if (workspace != null) {
	    if (workspace.getPlan() != null && workspace.getStatus().isActive()) {
	    	for (WSPermission perm: permissions) {
		    	switch (perm) {
	
		    		case PRODUCT_LIMIT: {
				      int allowed = (workspace.getPlan().getProductLimit() - workspace.getProductCount());
				      if (allowed < 1) {
				        res = Responses.NotAllowed.NO_PRODUCT_LIMIT;
				      }
							break;
						}
		
		    		case ALARM_LIMIT: {
				      int allowed = (workspace.getPlan().getAlarmLimit() - workspace.getAlarmCount());
				      if (allowed < 1) {
				      	res = Responses.NotAllowed.NO_ALARM_LIMIT;
				      }
							break;
						}
		
		    		case USER_LIMIT: {
				      int allowed = (workspace.getPlan().getUserLimit() - workspace.getUserCount());
				      if (allowed < 1) {
				      	res = Responses.NotAllowed.NO_USER_LIMIT;
				      }
							break;
						}
	
		    		default:
							break;
					}
		    	if (res.isOK() == false) break;
	    	}
	    } else {
	      res = Responses.NotAllowed.HAVE_NO_ACTIVE_PLAN;
	    }
    } else {
      res = Responses.NotFound.WORKSPACE;
    }
    return res;
  }

}
