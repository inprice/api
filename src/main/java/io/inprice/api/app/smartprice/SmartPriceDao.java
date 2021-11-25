package io.inprice.api.app.smartprice;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.app.smartprice.mapper.ProductSmartPrice;
import io.inprice.api.app.smartprice.mapper.ProductSmartPriceMapper;
import io.inprice.common.formula.SmartPriceDTO;
import io.inprice.common.info.IdName;
import io.inprice.common.mappers.IdNameMapper;
import io.inprice.common.mappers.SmartPriceMapper;
import io.inprice.common.models.SmartPrice;

public interface SmartPriceDao {
	
	final String FIELDS = ", sp.formula, sp.lower_limit_formula, sp.upper_limit_formula";

  @SqlUpdate(
		"insert into smart_price (name, formula, lower_limit_formula, upper_limit_formula, workspace_id) " +
		"values (:dto.name, :dto.formula, :dto.lowerLimitFormula, :dto.upperLimitFormula, :workspaceId)"
	)
  @GetGeneratedKeys()
  long insert(@BindBean("dto") SmartPriceDTO dto, @Bind("workspaceId") Long workspaceId);

  @SqlUpdate(
		"update smart_price set name=:dto.name, formula=:dto.formula, lower_limit_formula=:dto.lowerLimitFormula, " +
		"upper_limit_formula=:dto.upperLimitFormula " +
		"where id=:dto.id and workspace_id=:workspaceId"
	)
  boolean update(@BindBean("dto") SmartPriceDTO dto, @Bind("workspaceId") Long workspaceId);

  @SqlUpdate("delete from smart_price where id=:id and workspace_id=:workspaceId")
  boolean delete(@Bind("id") Long id, @Bind("workspaceId") Long workspaceId);

  @SqlQuery("select * from smart_price where id=:id and workspace_id=:workspaceId")
  @UseRowMapper(SmartPriceMapper.class)
  SmartPrice findById(@Bind("id") Long id, @Bind("workspaceId") Long workspaceId);

  @SqlQuery("select * from smart_price where name=:name and workspace_id=:workspaceId limit 1")
  @UseRowMapper(SmartPriceMapper.class)
  SmartPrice findByName(@Bind("name") String name, @Bind("workspaceId") Long workspaceId);

	@SqlQuery("select * from smart_price where name=:name and id!=:id and workspace_id=:workspaceId limit 1")
  @UseRowMapper(SmartPriceMapper.class)
	SmartPrice findByName(@Bind("name") String name, @Bind("id") Long otherThanThisId, @Bind("workspaceId") Long workspaceId);

  @SqlQuery("select id, name from smart_price where workspace_id=:workspaceId order by name")
  @UseRowMapper(IdNameMapper.class)
  List<IdName> list(@Bind("workspaceId") Long workspaceId);

  @SqlQuery(
		"select * from smart_price " +
		"where name like :term " +
		"  and workspace_id=:workspaceId " +
		"order by name"
	)
  @UseRowMapper(SmartPriceMapper.class)
  List<SmartPrice> search(@Bind("term") String term, @Bind("workspaceId") Long workspaceId);

  //in order to keep consistency for products, must be called after a smart price deleted
  @SqlUpdate(
		"update product " +
		"set smart_price_id=null, suggested_price=0, suggested_price_problem=null " +
		"where smart_price_id=:id " +
		"  and workspace_id=:workspaceId"
	)
  boolean releaseProducts(@Bind("id") Long id, @Bind("workspaceId") Long workspaceId);

  //to refresh all the bound products after an update
  @SqlQuery(
		"select p.id as product_id, p.actives, p.price, p.base_price, p.min_price, p.avg_price, p.max_price, " +
		"sp.formula, sp.lower_limit_formula, sp.lower_limit_formula from product as p " + 
	  "inner join smart_price as sp on sp.id = p.smart_price_id " + 
	  "where p.smart_price_id=:smartPriceId " +
	  "  and p.workspace_id=:workspaceId"  		
	)
  @UseRowMapper(ProductSmartPriceMapper.class)
	List<ProductSmartPrice> getSmartPricesWithProductId(@Bind("smartPriceId") Long smartPriceId, @Bind("workspaceId") Long workspaceId);

}
