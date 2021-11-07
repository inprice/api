package io.inprice.api.app.exim.link;

import java.util.HashMap;
import java.util.List;

import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import io.inprice.api.dto.LinkDTO;

public interface LinkDao {

	@SqlQuery("select exists(select 1 from link where url_hash=:dto.urlHash and product_id=:dto.productId and workspace_id=:dto.workspaceId)")
	boolean doesUrlHashExist(@BindBean("dto") LinkDTO dto);

  @SqlBatch(
		"insert into link (url, url_hash, product_id, workspace_id) " +
  	"values (:link.url, :link.urlHash, :link.productId, :link.workspaceId)"
	)
  void insertAll(@BindBean("link") List<LinkDTO> linkList);	

  @SqlQuery("select lower(sku) as sku, id from product where workspace_id=:workspaceId")
  @KeyColumn("sku")
  @ValueColumn("id")
  HashMap<String, Long> getProducts(@Bind("workspaceId") Long workspaceId);

}
