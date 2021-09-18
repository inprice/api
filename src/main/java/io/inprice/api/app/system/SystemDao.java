package io.inprice.api.app.system;

import java.util.List;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;

import io.inprice.api.app.system.mapper.SearchMapper;
import io.inprice.api.app.system.model.Search;
import io.inprice.common.info.PlanFeature;
import io.inprice.common.mappers.PlanFeatureReducer;
import io.inprice.common.mappers.PlanMapper;
import io.inprice.common.models.Plan;

public interface SystemDao {
	
	@SqlQuery("select * from plan where id=:id")
	@UseRowMapper(PlanMapper.class)
	Plan findById(@Bind("id") Integer id);

  @SqlQuery("select * from plan where name=:name")
  @UseRowMapper(PlanMapper.class)
  Plan findByName(@Bind("name") String name);

  @SqlQuery("select * from plan where type = 'PUBLIC'")
  @UseRowMapper(PlanMapper.class)
  List<Plan> findPublicPlans();
  
  @SqlQuery(
		"select p.id as p_id, p.type as p_type, p.name as p_name, f.description as p_description, p.price as p_price, p.user_limit as p_userLimit, p.link_limit as p_linkLimit, " +
		"p.alarm_limit as p_alarmLimit, f.id as f_id, f.description as f_description, f.allowed as f_allowed, f.order_no as f_orderNo from plan as p " +
		"inner join plans_and_features as pf on pf.plan_id = p.id " +
		"inner join plan_feature as f on f.id = pf.feature_id " +
		"where p.type = 'PUBLIC' " +
		"order by p.id, f.order_no"
	)
  @RegisterBeanMapper(value = Plan.class, prefix = "p")
  @RegisterBeanMapper(value = PlanFeature.class, prefix = "f")
  @UseRowReducer(PlanFeatureReducer.class)
  List<Plan> fetchPublicPlans();

  @SqlQuery(
		"(select 'L' as type, id, IFNULL(name, url) as name, seller as description from link where account_id=:accountId and IFNULL(name, url) like :term limit 10)" +
		" UNION " +
		"(select 'G' as type, id, name, description from product where account_id=:accountId and name like :term limit 10)"
	)
  @UseRowMapper(SearchMapper.class)
  List<Search> search(@Bind("term") String term, @Bind("accountId") Long accountId);

}
