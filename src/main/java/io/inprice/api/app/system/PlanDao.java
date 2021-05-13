package io.inprice.api.app.system;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.info.PlanFeature;
import io.inprice.common.mappers.PlanFeatureMapper;
import io.inprice.common.mappers.PlanMapper;
import io.inprice.common.models.Plan;

public interface PlanDao {
	
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
		"select f.description as feature, f.allowed from feature as f " +
		"inner join plan_feature as pf on pf.feature_id = f.id " +
		"inner join plan as p on p.id = pf.plan_id " +
		"where p.id = :planId " +
		"order by f.order_no"
	)
  @UseRowMapper(PlanFeatureMapper.class)
  List<PlanFeature> findFeaturesById(@Bind("planId") Integer planId);
  
  @SqlQuery(
		"select pf.plan_id as planId, f.description as feature, f.allowed from feature as f " +
		"inner join plan_feature as pf on pf.feature_id = f.id " +
		"inner join plan as p on p.id = pf.plan_id " +
		"where p.type = 'PUBLIC' " +
		"order by pf.plan_id, f.order_no"
	)
  @UseRowMapper(PlanFeatureMapper.class)
  List<PlanFeature> findAllFeaturesOfPublics();
  
}
