package io.inprice.api.helpers;

import org.jdbi.v3.core.Handle;

import io.inprice.api.app.workspace.WorkspaceDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.models.Workspace;

public class SystemHelper {

  public static Response checkAlarmLimit(Handle handle) {
    WorkspaceDao workspaceDao = handle.attach(WorkspaceDao.class);
    Workspace workspace = workspaceDao.findById(CurrentUser.getWorkspaceId());

    if (workspace.getPlan() != null) {
      int allowedAlarmCount = (workspace.getPlan().getAlarmLimit() - workspace.getAlarmCount());
      if (allowedAlarmCount < 1) {
        return Responses.NotAllowed.NO_ALARM_LIMIT;
      }
    } else {
      return Responses.NotAllowed.HAVE_NO_PLAN;
    }
    return Responses.OK;
  }

}
