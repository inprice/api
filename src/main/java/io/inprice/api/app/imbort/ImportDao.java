package io.inprice.api.app.imbort;

import java.util.List;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;

import io.inprice.api.app.imbort.mapper.ImportRowReducer;
import io.inprice.common.mappers.ImportDetailMapper;
import io.inprice.common.mappers.ImportMapper;
import io.inprice.common.models.Import;
import io.inprice.common.models.ImportDetail;

public interface ImportDao {

  @SqlUpdate("insert into import_ (type, company_id) values (:type, :companyId)")
  @GetGeneratedKeys
  long insert(@Bind("type") String type, @Bind("companyId") Long companyId);

  @SqlQuery(
    "select "+IMPORT_FIELDS+", "+IMPORT_DETAIL_FIELDS+" from import_ as i " +
    "left join import_detail as ir on ir.import_id = i.id " +
    "where i.id=:id and i.company_id=:companyId"
  )
  @RegisterBeanMapper(value = Import.class, prefix = "i")
  @RegisterBeanMapper(value = ImportDetail.class, prefix = "ir")
  @UseRowReducer(ImportRowReducer.class)
  Import findById(@Bind("id") Long id, @Bind("companyId") Long companyId);

  @SqlQuery("select * from import_ where company_id=:companyId order by id desc")
  @UseRowMapper(ImportMapper.class)
  List<Import> findListByCompanyId(@Bind("companyId") Long companyId);

  @SqlQuery("select * from import_detail where import_id=:importId and company_id=:companyId")
  @UseRowMapper(ImportDetailMapper.class)
  List<ImportDetail> findImportRowsByImportIdAndCompanyId(@Bind("importId") Long importId, @Bind("companyId") Long companyId);

  @SqlUpdate("insert into import_detail (data, eligible, imported, problem, import_id, company_id) "+
    "values (:ir.data, :ir.eligible, :ir.imported, :ir.problem, :ir.importId, :ir.companyId)")
  @GetGeneratedKeys
  long insertDetail(@BindBean("ir") ImportDetail ir);

  //look at ImportRowReducer class
  //these are necessary mappings since we need to establish one-to-one and one-to-many relations between tables
  final String IMPORT_FIELDS = 
    "i.id as i_id, " +
    "i.type as i_type, " +
    "i.row_count as i_row_count, " +
    "i.created_at as i_created_at ";

  final String IMPORT_DETAIL_FIELDS = 
    "ir.id as ir_id, " +
    "ir.data as ir_data, " +
    "ir.eligible as ir_eligible, " +
    "ir.imported as ir_imported, " +
    "ir.problem as ir_problem, " +
    "ir.last_check as ir_last_check, " +
    "ir.import_id as ir_import_id ";

}
