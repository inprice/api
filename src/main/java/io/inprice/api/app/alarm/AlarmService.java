package io.inprice.api.app.alarm;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.workspace.WorkspaceDao;
import io.inprice.api.app.alarm.dto.AlarmDTO;
import io.inprice.api.app.alarm.dto.OrderBy;
import io.inprice.api.app.alarm.dto.SearchDTO;
import io.inprice.api.app.link.LinkDao;
import io.inprice.api.app.product.ProductDao;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.info.Pair;
import io.inprice.common.mappers.AlarmMapper;
import io.inprice.common.meta.AlarmSubject;
import io.inprice.common.meta.AlarmSubjectWhen;
import io.inprice.common.meta.AlarmTopic;
import io.inprice.common.models.Workspace;
import io.inprice.common.models.Alarm;
import io.inprice.common.models.Link;
import io.inprice.common.models.Product;

/**
 * 
 * @since 2021-06-16
 * @author mdpinar
 */
public class AlarmService {

	private static final Logger logger = LoggerFactory.getLogger(AlarmService.class);

	Response insert(AlarmDTO dto) {
		Response res = Responses.Invalid.ALARM;
		
		String problem = validate(dto);

		if (problem == null) {
			try (Handle handle = Database.getHandle()) {
				
        WorkspaceDao workspaceDao = handle.attach(WorkspaceDao.class);
        Workspace workspace = workspaceDao.findById(CurrentUser.getWorkspaceId());

        if (workspace.getPlan() != null) {
          int allowedAlarmCount = (workspace.getPlan().getAlarmLimit() - workspace.getAlarmCount());
				
          if (allowedAlarmCount > 0) {
          	AlarmDao alarmDao = handle.attach(AlarmDao.class);
          	
          	boolean doesExist = 
          			alarmDao.doesExistByTopicId(
        					dto.getTopic().name().toLowerCase(), 
        					(AlarmTopic.LINK.equals(dto.getTopic()) ? dto.getLinkId() : dto.getProductId()), 
        					CurrentUser.getWorkspaceId()
      					);
          	
						if (doesExist == false) {
    					Pair<String, BigDecimal> pair = findCurrentStatusAndAmount(dto, handle);
    
    					if (pair != null) {
    						handle.begin();
    						long id = alarmDao.insert(dto, pair);
    
    						boolean isOK = false;
    						if (AlarmTopic.LINK.equals(dto.getTopic())) {
    							isOK = alarmDao.setAlarmForLink(dto.getLinkId(), id, CurrentUser.getWorkspaceId());
    						} else {
    							isOK = alarmDao.setAlarmForProduct(dto.getProductId(), id, CurrentUser.getWorkspaceId());
    						}
    
    						if (isOK) {
    		        	workspaceDao.increaseAlarmCount(CurrentUser.getWorkspaceId());
  
    		        	handle.commit();
    							dto.setId(id);
    							res = new Response(dto);
    						} else {
    							handle.rollback();
    							res = Responses.DataProblem.DB_PROBLEM;
    						}
  	          } else {
  	            res = (AlarmTopic.LINK.equals(dto.getTopic()) ? Responses.NotFound.LINK : Responses.NotFound.PRODUCT);
    					}
	          } else {
	          	res = Responses.Already.Defined.ALARM;
  					}
          } else {
	          res = Responses.NotAllowed.NO_ALARM_LIMIT;
          }
        } else {
          res = Responses.NotAllowed.HAVE_NO_PLAN;
        }
      }
		} else {
			res = new Response(problem);
		}

		return res;
	}

	Response update(AlarmDTO dto) {
		Response res = Responses.NotFound.ALARM;

		if (dto.getId() != null && dto.getId() > 0) {
			String problem = validate(dto);
			if (problem == null) {
				try (Handle handle = Database.getHandle()) {
					Pair<String, BigDecimal> pair = findCurrentStatusAndAmount(dto, handle);

					if (pair != null) {
						AlarmDao alarmDao = handle.attach(AlarmDao.class);
						boolean isOK = alarmDao.update(dto, pair);
						if (isOK) {
							res = new Response(dto);
						}
					}
				}
			} else {
				res = new Response(problem);
			}
		}
		return res;
	}

	Response delete(Long id) {
		Response res = Responses.NotFound.ALARM;

		if (id != null && id > 0) {
			try (Handle handle = Database.getHandle()) {
				AlarmDao alarmDao = handle.attach(AlarmDao.class);

				Alarm alarm = alarmDao.findById(id, CurrentUser.getWorkspaceId());
				if (alarm != null) {

					handle.begin();
					handle.execute("SET FOREIGN_KEY_CHECKS=0");

					boolean isOK = false;
					if (AlarmTopic.LINK.equals(alarm.getTopic())) {
						isOK = alarmDao.removeAlarmFromLink(alarm.getLinkId(), CurrentUser.getWorkspaceId());
					} else {
						isOK = alarmDao.removeAlarmFromProduct(alarm.getProductId(), CurrentUser.getWorkspaceId());
					}

					if (isOK) {
						isOK = alarmDao.delete(id, CurrentUser.getWorkspaceId());
						if (isOK) {
							handle.attach(WorkspaceDao.class).decreaseAlarmCount(CurrentUser.getWorkspaceId());

							handle.execute("SET FOREIGN_KEY_CHECKS=1");
							handle.commit();
							res = Responses.OK;
						} else {
							handle.execute("SET FOREIGN_KEY_CHECKS=1");
							handle.rollback();
							res = Responses.DataProblem.DB_PROBLEM;
						}
					} else {
						handle.execute("SET FOREIGN_KEY_CHECKS=1");
						handle.rollback();
						res = new Response("Parent record problem!");
					}
				}
			}
		}

		return res;
	}

	Response search(SearchDTO dto) {
		if (dto.getTerm() != null)
			dto.setTerm(SqlHelper.clear(dto.getTerm()));

		// ---------------------------------------------------
		// building the criteria up
		// ---------------------------------------------------
		StringBuilder where = new StringBuilder();

		where.append("where a.workspace_id = ");
		where.append(CurrentUser.getWorkspaceId());
		
		if (dto.getTopic() != null) {
			where.append(" and a.topic = '");
			where.append(dto.getTopic());
			where.append("'");
		}

		if (CollectionUtils.isNotEmpty(dto.getSubjects())) {
			where.append(
		    String.format(" and a.subject in (%s) ", io.inprice.common.utils.StringUtils.join("'", dto.getSubjects()))
	    );
		}

		if (CollectionUtils.isNotEmpty(dto.getWhens())) {
			where.append(
		    String.format(" and a.subject_when in (%s) ", io.inprice.common.utils.StringUtils.join("'", dto.getWhens()))
	    );
		}

		// limiting
		String limit = "";
		if (dto.getRowLimit() < Consts.LOWER_ROW_LIMIT_FOR_LISTS && dto.getRowLimit() <= Consts.UPPER_ROW_LIMIT_FOR_LISTS) {
			dto.setRowLimit(Consts.LOWER_ROW_LIMIT_FOR_LISTS);
		}
		if (dto.getRowLimit() > Consts.UPPER_ROW_LIMIT_FOR_LISTS) {
			dto.setRowLimit(Consts.UPPER_ROW_LIMIT_FOR_LISTS);
		}
		if (dto.getLoadMore()) {
			limit = " limit " + dto.getRowCount() + ", " + dto.getRowLimit();
		} else {
			limit = " limit " + dto.getRowLimit();
		}

		// ---------------------------------------------------
		// fetching the data
		// ---------------------------------------------------

		String selectForProducts = 
				"select a.*, g.name as _name from alarm a " +
		    "inner join product g on g.id = a.product_id " + generateNameLikeClause(dto, "g") +
		    where;
		
		String selectForLinks = 
				"select a.*, IFNULL(l.name, l.url) as _name from alarm a " +
		    "inner join link l on l.id = a.link_id " + generateNameLikeClause(dto, "l") +
		    where;

		String select = null;
		
		if (AlarmTopic.PRODUCT.equals(dto.getTopic())) {
			select = selectForProducts;
		} else if (AlarmTopic.LINK.equals(dto.getTopic())) {
			select = selectForLinks;
		} else {
			select = selectForProducts + " union " + selectForLinks;
		}

		String orderBy = " order by " + dto.getOrderBy().getFieldName() + dto.getOrderDir().getDir();
		if (! OrderBy.NAME.equals(dto.getOrderBy())) {
			orderBy += ", _name";
		}

		try (Handle handle = Database.getHandle()) {
			List<Alarm> searchResult = 
				handle
			    .createQuery(
		        select + 
		        orderBy + ", id " + 
		        limit
	        )
		    .map(new AlarmMapper()).list();

			return new Response(searchResult);
		} catch (Exception e) {
			logger.error("Failed in full search for alarms.", e);
			return Responses.ServerProblem.EXCEPTION;
		}
	}

	private String generateNameLikeClause(SearchDTO dto, String prefix) {
		StringBuilder sb = new StringBuilder();
		if (StringUtils.isNotBlank(dto.getTerm())) {
			sb.append(" and ");
			if ("l".equals(prefix)) {
				sb.append("(");
			}
			sb.append(prefix);
			sb.append(".name like '%");
			sb.append(dto.getTerm());
			sb.append("%' ");
			if ("l".equals(prefix)) {
				sb.append("or (l.name is null ");
				sb.append(" and l.url like '%");
				sb.append(dto.getTerm());
				sb.append("%')) ");
			}
		}
		return sb.toString();
	}

	private String validate(AlarmDTO dto) {
		String problem = null;

		if (dto.getTopic() == null) {
			problem = "Topic cannot be empty!";
		}

		if (problem == null && dto.getProductId() == null && dto.getLinkId() == null) {
			problem = "Topic id cannot be empty!";
		}

		if (problem == null && dto.getSubject() == null) {
			problem = "Subject cannot be empty!";
		}

		if (problem == null && dto.getSubjectWhen() == null) {
			problem = "You are expected to specify when the subject should be considered!";
		}

		if (problem == null) {
			if (AlarmSubject.STATUS.equals(dto.getSubject()) && !AlarmSubjectWhen.CHANGED.equals(dto.getSubjectWhen())) {
				if (StringUtils.isBlank(dto.getCertainStatus())) {
					problem = "You are expected to specify a certain status!";
				}
			} else {
				dto.setCertainStatus(null);
			}
		}

		if (problem == null) {
			if (!AlarmSubject.STATUS.equals(dto.getSubject()) && AlarmSubjectWhen.OUT_OF_LIMITS.equals(dto.getSubjectWhen())) {
			
				boolean hasNoLowerLimit = 
						(dto.getAmountLowerLimit() == null 
						|| dto.getAmountLowerLimit().compareTo(BigDecimal.ZERO) < 1 
						|| dto.getAmountLowerLimit().compareTo(new BigDecimal(9_999_999)) > 0);

				boolean hasNoUpperLimit = 
						(dto.getAmountUpperLimit() == null 
						|| dto.getAmountUpperLimit().compareTo(BigDecimal.ZERO) < 1 
						|| dto.getAmountUpperLimit().compareTo(new BigDecimal(9_999_999)) > 0);

				if (hasNoLowerLimit && hasNoUpperLimit) {
					problem = "You are expected to specify either lower or upper limit for " + dto.getSubject().name().toLowerCase() + "!";
				} else {
					if (hasNoLowerLimit) dto.setAmountLowerLimit(BigDecimal.ZERO);
					if (hasNoUpperLimit) dto.setAmountUpperLimit(BigDecimal.ZERO);
				}
			} else {
				dto.setAmountLowerLimit(BigDecimal.ZERO);
				dto.setAmountUpperLimit(BigDecimal.ZERO);
			}
		}

		if (problem == null) {
			dto.setWorkspaceId(CurrentUser.getWorkspaceId());
		}

		return problem;
	}

	private Pair<String, BigDecimal> findCurrentStatusAndAmount(AlarmDTO dto, Handle handle) {
		Pair<String, BigDecimal> pair = null;
		
		switch (dto.getTopic()) {
			case LINK: {
  			LinkDao linkDao = handle.attach(LinkDao.class);
  			Link link = linkDao.findById(dto.getLinkId(), CurrentUser.getWorkspaceId());
  			if (link != null) {
  				pair = new Pair<>();
  				pair.setLeft(link.getGrup().name());
  				pair.setRight(link.getPrice());
  			}
  			break;
			}

			case PRODUCT: {
  			ProductDao productDao = handle.attach(ProductDao.class);
  			Product product = productDao.findById(dto.getProductId(), CurrentUser.getWorkspaceId());
  			if (product != null) {
  				pair = new Pair<>();
  				pair.setLeft(product.getLevel().name());
  				switch (dto.getSubject()) {
  				case MINIMUM: {
  					pair.setRight(product.getMinPrice());
  					break;
  				}
  				case AVERAGE: {
  					pair.setRight(product.getAvgPrice());
  					break;
  				}
  				case MAXIMUM: {
  					pair.setRight(product.getMaxPrice());
  					break;
  				}
  				case TOTAL: {
  					pair.setRight(product.getTotal());
  					break;
  				}
  				default:
  					pair.setRight(BigDecimal.ZERO);
  				}
  			}
  			break;
			}
		}

		return pair;
	}

}
