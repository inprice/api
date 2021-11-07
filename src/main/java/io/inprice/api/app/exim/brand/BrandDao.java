package io.inprice.api.app.exim.brand;

import java.util.List;
import java.util.Set;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

public interface BrandDao {

	@SqlQuery("select exists(select 1 from brand where name=:name and workspace_id=:workspaceId)")
	boolean doesNameExist(@Bind("name") String name, @Bind("workspaceId") Long workspaceId);

  @SqlBatch("insert into brand (name, workspace_id) values (?, ?)")
  void insertAll(Set<String> names, Long workspaceId);	

  @SqlQuery("select name from brand where workspace_id=:workspaceId order by name")
  List<String> getList(@Bind("workspaceId") Long workspaceId);

}
