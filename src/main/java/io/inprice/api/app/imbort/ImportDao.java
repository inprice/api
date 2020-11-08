package io.inprice.api.app.imbort;

import java.util.List;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;

import io.inprice.api.app.imbort.mapper.ImportRowReducer;
import io.inprice.common.info.ImportRow;
import io.inprice.common.mappers.ImportMapper;
import io.inprice.common.mappers.LinkMapper;
import io.inprice.common.models.Import;
import io.inprice.common.models.Link;

public interface ImportDao {

  @SqlUpdate("insert into imbort (type, company_id) values (:type, :companyId)")
  @GetGeneratedKeys
  long insert(@Bind("type") String type, @Bind("companyId") Long companyId);

  @SqlQuery(
    "select "+IMPORT_FIELDS+", "+IMPORT_ROW_FIELDS+" from imbort as i " +
    "left join link as ir on ir.imbort_id = i.id " +
    "where i.id=:id and i.company_id=:companyId"
  )
  @RegisterBeanMapper(value = Import.class, prefix = "i")
  @RegisterBeanMapper(value = ImportRow.class, prefix = "ir")
  @UseRowReducer(ImportRowReducer.class)
  Import findById(@Bind("id") Long id, @Bind("companyId") Long companyId);

  @SqlQuery("select * from imbort where company_id=:companyId order by id desc")
  @UseRowMapper(ImportMapper.class)
  List<Import> findListByCompanyId(@Bind("companyId") Long companyId);

  @SqlQuery("select * from link where imbort_id=:imbortId and company_id=:companyId order by status")
  @UseRowMapper(LinkMapper.class)
  List<Link> findImportRowsByImportIdAndCompanyId(@Bind("imbortId") Long importId, @Bind("companyId") Long companyId);

  //look at ImportRowReducer class
  //these are necessary mappings since we need to establish one-to-one and one-to-many relations between tables
  final String IMPORT_FIELDS = 
    "i.id as i_id, " +
    "i.type as i_type, " +
    "i.created_at as i_created_at ";

  final String IMPORT_ROW_FIELDS = 
    "l.id as ir_id, " +
    "l.url as ir_data, " +
    "l.last_check as ir_last_check, " +
    "l.last_update as ir_last_update, " +
    "l.status as ir_status, " +
    "l.problem as ir_problem, " +
    "l.retry as ir_retry, " +
    "l.http_status as ir_http_status, " +
    "l.import_type as ir_import_type ";

}
