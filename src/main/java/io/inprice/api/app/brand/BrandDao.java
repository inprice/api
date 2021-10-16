package io.inprice.api.app.brand;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.info.IdName;
import io.inprice.common.mappers.BrandMapper;
import io.inprice.common.mappers.IdNameMapper;
import io.inprice.common.models.Brand;

public interface BrandDao {

  final String FIELDS = ", brn.id as brand_id, brn.name as brand_name ";
	
  @SqlUpdate("insert into brand (name, workspace_id) values (:name, :workspaceId)")
  @GetGeneratedKeys()
  long insert(@Bind("name") String name, @Bind("workspaceId") Long workspaceId);

  @SqlUpdate("update brand set name=:name where id=:id and workspace_id=:workspaceId")
  boolean update(@Bind("id") Long id, @Bind("name") String name, @Bind("workspaceId") Long workspaceId);

  @SqlUpdate("delete from brand where id=:id and workspace_id=:workspaceId")
  boolean delete(@Bind("id") Long id, @Bind("workspaceId") Long workspaceId);

  @SqlQuery("select * from brand where id=:id and workspace_id=:workspaceId")
  @UseRowMapper(BrandMapper.class)
  Brand findById(@Bind("id") Long id, @Bind("workspaceId") Long workspaceId);

  @SqlQuery("select * from brand where name=:name and workspace_id=:workspaceId limit 1")
  @UseRowMapper(BrandMapper.class)
  Brand findByName(@Bind("name") String name, @Bind("workspaceId") Long workspaceId);

	@SqlQuery("select * from brand where name=:name and id!=:id and workspace_id=:workspaceId limit 1")
	@UseRowMapper(BrandMapper.class)
	Brand findByName(@Bind("name") String name, @Bind("id") Long otherThanThisId, @Bind("workspaceId") Long workspaceId);

  @SqlQuery("select id, name from brand where workspace_id=:workspaceId order by name")
  @UseRowMapper(IdNameMapper.class)
  List<IdName> list(@Bind("workspaceId") Long workspaceId);

  @SqlQuery(
		"select * from brand " +
		"where name like :term " +
		"  and workspace_id=:workspaceId " +
		"order by name"
	)
  @UseRowMapper(BrandMapper.class)
  List<Brand> search(@Bind("term") String term, @Bind("workspaceId") Long workspaceId);

  //in order to keep consistency for products, must be called after a brand deleted
  @SqlUpdate("update product set brand_id=null where brand_id=:id and workspace_id=:workspaceId")
  boolean releaseProducts(@Bind("id") Long id, @Bind("workspaceId") Long workspaceId);

}
