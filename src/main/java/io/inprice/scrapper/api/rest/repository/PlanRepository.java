package io.inprice.scrapper.api.rest.repository;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.common.models.Plan;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PlanRepository {

    private static final Logger log = LoggerFactory.getLogger(CompanyRepository.class);
    private final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);

    public int findAllowedProductCount() {
        int result = 0;

        ServiceResponse<Plan> serres = findByWorkspaceId();
        if (serres.isOK()) {
            result = serres.getModel().getRowLimit();
        } else {
            result = -1;
        }

        return result;
    }

    private ServiceResponse findByWorkspaceId() {
        ServiceResponse<Plan> response = InstantResponses.CRUD_ERROR("");

        Plan model =
            dbUtils.findSingle(
            "select p.* from plan as p " +
                    "inner join workspace as ws on p.id = ws.plan_id " +
                    "where ws.id="+ Context.getWorkspaceId(), PlanRepository::map);
        if (model != null) {
            response.setStatus(HttpStatus.OK_200);
            response.setModel(model);
        }

        return response;
    }

    private static Plan map(ResultSet rs) {
        try {
            Plan model = new Plan();
            model.setId(rs.getLong("id"));
            model.setName(rs.getString("name"));
            model.setPrice(rs.getBigDecimal("price"));
            model.setRowLimit(rs.getInt("row_limit"));
            model.setOrderNo(rs.getInt("order_no"));

            return model;
        } catch (SQLException e) {
            log.error("Failed to set plan's properties", e);
        }
        return null;
    }

}
