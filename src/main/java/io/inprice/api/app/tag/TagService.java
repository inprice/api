package io.inprice.api.app.tag;

import org.jdbi.v3.core.Handle;

import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;

public class TagService {

  public Response findAll() {
    try (Handle handle = Database.getHandle()) {
      TagDao tagDao = handle.attach(TagDao.class);
      return new Response(tagDao.findAll(CurrentUser.getAccountId()));
    }
  }

}
