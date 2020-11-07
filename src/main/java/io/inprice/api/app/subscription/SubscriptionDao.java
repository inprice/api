package io.inprice.api.app.subscription;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.SubsTransMapper;
import io.inprice.common.models.SubsTrans;

public interface SubscriptionDao {

  @SqlQuery("select * from subs_trans where event_id=:eventId")
  @UseRowMapper(SubsTransMapper.class)
  SubsTrans findByEventId(@Bind("eventId") String eventId);

  @SqlQuery("select * from subs_trans where company_id=:companyId order by id desc")
  @UseRowMapper(SubsTransMapper.class)
  List<SubsTrans> findListByCompanyId(@Bind("companyId") Long companyId);

  @SqlUpdate(
    "insert into subs_trans (company_id, event_id, successful, reason, description, file_url, event_source, event) " + 
    "values (:trans.companyId, :trans.eventId, :trans.successful, :trans.reason, :trans.description, :trans.fileUrl, :eventSource, :event)"
  )
  boolean insertTrans(@BindBean("trans") SubsTrans trans, @Bind("eventSource") String eventSource, @Bind("event") String event);

}
