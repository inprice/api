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

  @SqlQuery("select * from subs_trans where company_id=:companyId order by created_at desc")
  @UseRowMapper(SubsTransMapper.class)
  List<SubsTrans> findTransListByCompanyId(@Bind("companyId") Long companyId);

  @SqlUpdate(
    "insert into subs_trans (company_id, event_source, event_id, event, successful, reason, description, file_url) " + 
    "values (:trans.companyId, :eventSource, :trans.eventId, :event, :trans.successful, :trans.reason, :trans.description, :trans.fileUrl)"
  )
  boolean insertTrans(@BindBean("trans") SubsTrans trans, @Bind("eventSource") String eventSource, @Bind("event") String event);

}
