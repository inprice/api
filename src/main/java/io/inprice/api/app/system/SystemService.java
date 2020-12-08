package io.inprice.api.app.system;

import org.jdbi.v3.core.Handle;

import io.inprice.api.app.company.CompanyDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.helpers.Commons;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.config.Plans;
import io.inprice.common.helpers.Database;
import io.inprice.common.models.Company;

public class SystemService {

  Response getPlans() {
    return new Response(Plans.getPlans());
  }

  Response refreshSession() {
    Response res = Responses.NotFound.COMPANY;

    try (Handle handle = Database.getHandle()) {
      CompanyDao companyDao = handle.attach(CompanyDao.class);
      Company company = companyDao.findById(CurrentUser.getCompanyId());

      if (company != null) {
        res = Commons.refreshSession(company);
      }
    } catch (Exception e) {
      res = Responses.DataProblem.DB_PROBLEM;
    }

    return res;
  }

}
