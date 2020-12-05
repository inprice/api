package io.inprice.api.app.subscription;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.CompanyHistoryMapper;
import io.inprice.common.mappers.CompanyTransMapper;
import io.inprice.common.models.CompanyHistory;
import io.inprice.common.models.CompanyTrans;

public interface SubscriptionDao {

  @SqlQuery("select * from company_trans where event_id=:eventId")
  @UseRowMapper(CompanyTransMapper.class)
  CompanyTrans findByEventId(@Bind("eventId") String eventId);

  @SqlQuery("select * from company_trans where company_id=:companyId order by id desc")
  @UseRowMapper(CompanyTransMapper.class)
  List<CompanyTrans> findListByCompanyId(@Bind("companyId") Long companyId);

  @SqlUpdate(
    "insert into company_trans (company_id, event_id, successful, reason, description, file_url, event) " + 
    "values (:trans.companyId, :trans.eventId, :trans.successful, :trans.reason, :trans.description, :trans.fileUrl, :event)"
  )
  boolean insertTrans(@BindBean("trans") CompanyTrans trans, @Bind("event") String event);

  @SqlUpdate("insert into company_history (company_id, status) values (:companyId, :status)")
  boolean insertCompanyStatusHistory(@Bind("companyId") Long companyId, @Bind("status") String status);

  @SqlQuery("select * from company_history where company_id=:companyId and status=:status order by id desc limit 1")
  @UseRowMapper(CompanyHistoryMapper.class)
  CompanyHistory findPreviousHistoryRowByStatusAndCompanyId(@Bind("companyId") Long companyId, @Bind("status") String status);

}
