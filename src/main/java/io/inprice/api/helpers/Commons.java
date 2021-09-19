package io.inprice.api.helpers;

import java.util.Map;

import org.jdbi.v3.core.Handle;

import io.inprice.api.app.workspace.WorkspaceDao;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.session.info.ForResponse;
import io.inprice.common.helpers.Database;
import io.inprice.common.models.Workspace;

public class Commons {

  public static Response refreshSession(Long workspaceId) {
    try (Handle handle = Database.getHandle()) {
      WorkspaceDao workspaceDao = handle.attach(WorkspaceDao.class);
      return refreshSession(workspaceDao, workspaceId);
    }
  }

  public static Response refreshSession(WorkspaceDao workspaceDao, Long workspaceId) {
    Workspace workspace = workspaceDao.findById(workspaceId);
    return refreshSession(workspace);
  }

  public static Response refreshSession(Workspace workspace) {
    ForResponse session = new ForResponse(
      workspace,
      CurrentUser.getUserName(),
      CurrentUser.getEmail(),
      CurrentUser.getRole(),
      CurrentUser.getUserTimezone()
    );
    return new Response(Map.of("session", session));
  }

}
