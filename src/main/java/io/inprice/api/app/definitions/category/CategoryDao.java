package io.inprice.api.app.definitions.category;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.CategoryMapper;
import io.inprice.common.models.Category;

public interface CategoryDao {

  @SqlUpdate("insert into category (name, workspace_id) values (:name, :workspaceId)")
  @GetGeneratedKeys()
  long insert(@Bind("name") String name, @Bind("workspaceId") Long workspaceId);

  @SqlUpdate("update category set name=:name where id=:id and workspace_id=:workspaceId")
  boolean update(@Bind("id") Long id, @Bind("name") String name, @Bind("workspaceId") Long workspaceId);

  @SqlUpdate("delete from category where id=:id and workspace_id=:workspaceId")
  boolean delete(@Bind("id") Long id, @Bind("workspaceId") Long workspaceId);

  @SqlQuery("select * from category where id=:id and workspace_id=:workspaceId")
  @UseRowMapper(CategoryMapper.class)
  Category findById(@Bind("id") Long id, @Bind("workspaceId") Long workspaceId);

  @SqlQuery("select * from category where name=:name and workspace_id=:workspaceId limit 1")
  @UseRowMapper(CategoryMapper.class)
  Category findByName(@Bind("name") String name, @Bind("workspaceId") Long workspaceId);

	@SqlQuery("select * from category where name=:name and id!=:id and workspace_id=:workspaceId limit 1")
	@UseRowMapper(CategoryMapper.class)
	Category findByName(@Bind("name") String name, @Bind("id") Long otherThanThisId, @Bind("workspaceId") Long workspaceId);

  @SqlQuery("select * from category where workspace_id=:workspaceId order by name")
  @UseRowMapper(CategoryMapper.class)
  List<Category> list(@Bind("workspaceId") Long workspaceId);

  @SqlQuery(
		"select * from category " +
		"where name like :term " +
		"  and workspace_id=:workspaceId " +
		"order by name"
	)
  @UseRowMapper(CategoryMapper.class)
  List<Category> search(@Bind("term") String term, @Bind("workspaceId") Long workspaceId);

  //in order to keep consistency for products, must be called after a category deleted
  @SqlUpdate("update product set category_id=null where category_id=:id and workspace_id=:workspaceId")
  boolean releaseProducts(@Bind("id") Long id, @Bind("workspaceId") Long workspaceId);

}
