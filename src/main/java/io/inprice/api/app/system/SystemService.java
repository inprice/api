package io.inprice.api.app.system;

import org.jdbi.v3.core.Handle;

import io.inprice.api.app.company.CompanyDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.helpers.Commons;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.config.Plans;
import io.inprice.common.helpers.Database;

public class SystemService {

  Response getPlans() {
    return new Response(Plans.getPlans());
  }

  Response refreshSession() {
    Response res = Responses.NotFound.COMPANY;

    try (Handle handle = Database.getHandle()) {
      CompanyDao companyDao = handle.attach(CompanyDao.class);
      res = Commons.refreshSession(companyDao, CurrentUser.getCompanyId());
    } catch (Exception e) {
      res = Responses.DataProblem.DB_PROBLEM;
    }

    return res;
  }

}
