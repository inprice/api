package io.inprice.api.app.system;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.PlanMapper;
import io.inprice.common.models.Plan;

public interface PlanDao {
	
	@SqlQuery("select * from plan where id=:id")
	@UseRowMapper(PlanMapper.class)
	Plan findById(@Bind("id") Integer id);

  @SqlQuery("select * from plan where name=:name")
  @UseRowMapper(PlanMapper.class)
  Plan findByName(@Bind("name") String name);

  @SqlQuery("select * from plan where is_standard = true")
  @UseRowMapper(PlanMapper.class)
  List<Plan> findStandardList();

}
