package io.inprice.api.app.imbort;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Batch;

import io.inprice.api.consts.Responses;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.ImportType;
import io.inprice.common.models.Import;
import io.inprice.common.models.ImportDetail;

public class BaseImportService {

  Response upload(String content, boolean isFile) {
    return Responses.DataProblem.NOT_SUITABLE;
  }

  Response upload(ImportType importType, String content, boolean isFile) {
    return Responses.DataProblem.NOT_SUITABLE;
  }

  Response findById(Long importId) {
    try (Handle handle = Database.getHandle()) {
      ImportDao importDao = handle.attach(ImportDao.class);

      Import imbort = importDao.findById(importId, CurrentUser.getCompanyId());
      if (imbort != null) {
        return new Response(imbort);
      }
      return Responses.NotFound.PRODUCT;
    }
  }

  Response getList() {
    Response res = Responses.NotFound.IMPORT;
    try (Handle handle = Database.getHandle()) {
      ImportDao importDao = handle.attach(ImportDao.class);

      List<Import> list = importDao.findListByCompanyId(CurrentUser.getCompanyId());
      if (list != null && list.size() > 0) {
        res = new Response(list);
      }
    }
    return res;
  }

  Response getDetailsList(Long id) {
    Response res = Responses.NotFound.IMPORT;
    try (Handle handle = Database.getHandle()) {
      ImportDao importDao = handle.attach(ImportDao.class);

      Import imbort = importDao.findById(id, CurrentUser.getCompanyId());
      if (imbort != null) {
        List<ImportDetail> list = importDao.findImportRowsByImportId(id, CurrentUser.getCompanyId());
        if (list != null && list.size() > 0) {
          Map<String, Object> data = new HashMap<>(2);
          data.put("import", imbort);
          data.put("list", list);
          res = new Response(data);
        }
      }
    }
    return res;
  }

  Response deleteById(Long importId) {
    if (importId != null && importId > 0) {
      final boolean[] isOK = { false };

      String where =
        String.format("where import_id=%d and company_id=%d", importId, CurrentUser.getCompanyId());

      try (Handle handle = Database.getHandle()) {
        handle.inTransaction(transactional -> {
          Batch batch = transactional.createBatch();
          batch.add("delete from link where import_detail_id in (select id from import_detail " + where + ")");
          batch.add("delete from import_detail " + where);
          batch.add("delete from import_ " + where.replaceAll("import_", "")); //this clause is important since determines the success!
          int[] result = batch.execute();
          isOK[0] = (result[2] > 0);
          return isOK[0];
        });
      }

      if (isOK[0]) {
        return Responses.OK;
      }
    }
    return Responses.Invalid.PRODUCT;
  }

}