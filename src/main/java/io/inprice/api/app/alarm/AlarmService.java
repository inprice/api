package io.inprice.api.app.alarm;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.alarm.dto.AlarmDTO;
import io.inprice.api.app.alarm.dto.SearchDTO;
import io.inprice.api.app.workspace.WorkspaceDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.utils.DTOHelper;
import io.inprice.common.helpers.Database;
import io.inprice.common.mappers.AlarmMapper;
import io.inprice.common.meta.AlarmSubject;
import io.inprice.common.meta.AlarmSubjectWhen;
import io.inprice.common.models.Alarm;
import io.inprice.common.models.Workspace;
import io.inprice.common.utils.StringHelper;

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

        		String name = generateName(dto);
          	boolean doesExist = alarmDao.doesExistByName(name, 0l, CurrentUser.getWorkspaceId());
          	
						if (doesExist == false) {
    					dto.setName(name);

    					handle.begin();
  						long id = alarmDao.insert(dto);
  						if (id > 0) {
  		        	workspaceDao.incAlarmCount(CurrentUser.getWorkspaceId());
  		        	handle.commit();
  							dto.setId(id);
  							res = new Response(dto);
  						} else {
  							handle.rollback();
  							res = Responses.DataProblem.DB_PROBLEM;
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
        	AlarmDao alarmDao = handle.attach(AlarmDao.class);

      		String name = generateName(dto);
      		boolean doesExist = alarmDao.doesExistByName(name, dto.getId(), CurrentUser.getWorkspaceId());

					if (doesExist == false) {
						dto.setName(name);

						handle.begin();
						boolean isOK = alarmDao.update(dto);
						if (isOK) {
							alarmDao.resetAlarm("product", dto.getId(), CurrentUser.getWorkspaceId());
							alarmDao.resetAlarm("link", dto.getId(), CurrentUser.getWorkspaceId());
							handle.commit();
							res = new Response(dto);
						} else {
							handle.rollback();
						}
          } else {
          	res = Responses.Already.Defined.ALARM;
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
					alarmDao.removeAlarm("product", id, CurrentUser.getWorkspaceId());
					alarmDao.removeAlarm("link", id, CurrentUser.getWorkspaceId());
					handle.attach(WorkspaceDao.class).decAlarmCount(CurrentUser.getWorkspaceId());
					handle.execute("SET FOREIGN_KEY_CHECKS=1");
					handle.commit();
					res = Responses.OK;
				}
			}
		}

		return res;
	}

	Response search(SearchDTO dto) {
  	dto = DTOHelper.normalizeSearch(dto, true);

		// ---------------------------------------------------
		// building the criteria up
		// ---------------------------------------------------
		StringBuilder where = new StringBuilder();

		where.append("where workspace_id = ");
		where.append(CurrentUser.getWorkspaceId());

    if (StringUtils.isNotBlank(dto.getTerm())) {
    	where.append(" and name");
      where.append(" like '%");
      where.append(dto.getTerm());
      where.append("%' ");
    }

		if (dto.getTopic() != null) {
			where.append(" and topic = '");
			where.append(dto.getTopic());
			where.append("'");
		}

		if (CollectionUtils.isNotEmpty(dto.getSubjects())) {
			where.append(
		    String.format(" and subject in (%s) ", StringHelper.join("'", dto.getSubjects()))
	    );
		}

		if (CollectionUtils.isNotEmpty(dto.getWhens())) {
			where.append(
		    String.format(" and subject_when in (%s) ", StringHelper.join("'", dto.getWhens()))
	    );
		}

		// limiting
		String limit = "";
		if (dto.getLoadMore()) {
			limit = " limit " + dto.getRowCount() + ", " + dto.getRowLimit();
		} else {
			limit = " limit " + dto.getRowLimit();
		}

		try (Handle handle = Database.getHandle()) {
			List<Alarm> searchResult = 
				handle
			    .createQuery(
		        "select * from alarm " +
			      where +
		        " order by " + dto.getOrderBy().getFieldName() + dto.getOrderDir().getDir() + 
		        limit
	        )
		    .map(new AlarmMapper()).list();

			return new Response(searchResult);
		} catch (Exception e) {
			logger.error("Failed in full search for alarms.", e);
			return Responses.ServerProblem.EXCEPTION;
		}
	}

	private String validate(AlarmDTO dto) {
		String problem = null;

		if (dto.getTopic() == null) {
			problem = "Topic cannot be empty!";
		}

		if (problem == null && dto.getSubject() == null) {
			problem = "Subject cannot be empty!";
		}

		if (problem == null && dto.getSubjectWhen() == null) {
			problem = "You are expected to specify when the subject should be considered!";
		}

		if (problem == null) {
			if (AlarmSubject.POSITION.equals(dto.getSubject()) && AlarmSubjectWhen.CHANGED.equals(dto.getSubjectWhen()) == false) {
				if (StringUtils.isBlank(dto.getCertainPosition())) {
					problem = "You are expected to specify a certain position!";
				}
			} else {
				dto.setCertainPosition(null);
			}
		}

		if (problem == null) {
			if (AlarmSubject.POSITION.equals(dto.getSubject()) == false && AlarmSubjectWhen.OUT_OF_LIMITS.equals(dto.getSubjectWhen())) {
			
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

	private String generateName(AlarmDTO dto) {
		StringBuilder sb = new StringBuilder();

		sb.append(dto.getTopic());
		sb.append(" ");

		sb.append(dto.getSubject());
		sb.append(" ");

		switch (dto.getSubject()) {
			case MINIMUM:
			case AVERAGE:
			case MAXIMUM: {
				sb.append("price");
				break;
			}
			default: break;
		}

		switch (dto.getSubjectWhen()) {
			case EQUAL:
			case NOT_EQUAL: {
				sb.append(dto.getSubjectWhen());
				sb.append("s to ");
				sb.append(dto.getCertainPosition());
				break;
			}
			case OUT_OF_LIMITS: {
				sb.append(" is ");
				boolean hasLowerLimit = (dto.getAmountLowerLimit() != null && dto.getAmountLowerLimit().compareTo(BigDecimal.ZERO) > 0);
				boolean hasUpperLimit = (dto.getAmountUpperLimit() != null && dto.getAmountUpperLimit().compareTo(BigDecimal.ZERO) > 0);
				if (hasLowerLimit && hasUpperLimit) {
					sb.append(" less than ");
					sb.append(dto.getAmountLowerLimit());
					sb.append(" or greater than ");
					sb.append(dto.getAmountUpperLimit());
				} else if (hasLowerLimit) {
					sb.append(" greater than ");
					sb.append(dto.getAmountLowerLimit());
				} else if (hasUpperLimit) {
					sb.append(" less than ");
					sb.append(dto.getAmountUpperLimit());
				}
				break;
			}

			default: {
				sb.append(" is ");
				sb.append(dto.getSubjectWhen());
				break;
			}
		}

		return StringUtils.capitalize(sb.toString().toLowerCase().replaceAll("_",  " "));
	}

}
