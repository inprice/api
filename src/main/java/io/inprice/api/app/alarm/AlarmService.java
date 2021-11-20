package io.inprice.api.app.alarm;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.alarm.dto.AlarmDTO;
import io.inprice.api.app.alarm.dto.SetAlarmOFFDTO;
import io.inprice.api.app.alarm.mapper.AlarmEntity;
import io.inprice.api.app.workspace.WorkspaceDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.utils.DTOHelper;
import io.inprice.common.helpers.Database;
import io.inprice.common.mappers.AlarmMapper;
import io.inprice.common.meta.AlarmSubject;
import io.inprice.common.meta.AlarmSubjectWhen;
import io.inprice.common.meta.AlarmTopic;
import io.inprice.common.models.Alarm;

/**
 * 
 * @since 2021-06-16
 * @author mdpinar
 */
public class AlarmService {

	private static final Logger logger = LoggerFactory.getLogger(AlarmService.class);

	Response getDetails(Long id) {
		Response res = Responses.NotFound.ALARM;

		if (id != null && id > 0) {
			try (Handle handle = Database.getHandle()) {
				AlarmDao alarmDao = handle.attach(AlarmDao.class);

				Alarm alarm = alarmDao.findById(id, CurrentUser.getWorkspaceId());
				if (alarm != null) {
					List<AlarmEntity> products = alarmDao.findProductEntities(id, CurrentUser.getWorkspaceId());
					List<AlarmEntity> links = alarmDao.findLinkEntities(id, CurrentUser.getWorkspaceId());

					Map<String, Object> dataMap = Map.of(
						"alarm", alarm,
						"products", products,
						"links", links
					);
					res = new Response(dataMap);
				}
			}
		}

		return res;
	}

  Response getIdNameList(AlarmTopic topic) {
  	try (Handle handle = Database.getHandle()) {
  		AlarmDao alarmDao = handle.attach(AlarmDao.class);
  		return new Response(alarmDao.getIdNameList(topic, CurrentUser.getWorkspaceId()));
  	}
  }

	Response insert(AlarmDTO dto) {
		Response res = Responses.Invalid.ALARM;
		
		String problem = validate(dto);

		if (problem == null) {
			try (Handle handle = Database.getHandle()) {
				AlarmDao alarmDao = handle.attach(AlarmDao.class);

    		String name = generateName(dto);
      	boolean doesExist = alarmDao.doesExistByName(name, 0l, CurrentUser.getWorkspaceId());

				if (doesExist == false) {
					dto.setName(name);
					long id = alarmDao.insert(dto);
					if (id > 0) {
						dto.setId(id);
						res = new Response(dto);
					} else {
						res = Responses.DataProblem.DB_PROBLEM;
					}
        } else {
        	res = Responses.Already.Defined.ALARM;
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
							alarmDao.resetAlarmById("product", dto.getId(), CurrentUser.getWorkspaceId());
							alarmDao.resetAlarmById("link", dto.getId(), CurrentUser.getWorkspaceId());
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
					int productAlarms = alarmDao.removeAlarmById("product", id, CurrentUser.getWorkspaceId());
					int linkAlarms = alarmDao.removeAlarmById("link", id, CurrentUser.getWorkspaceId());

					if (productAlarms + linkAlarms > 0) {
						WorkspaceDao workspaceDao = handle.attach(WorkspaceDao.class);
						workspaceDao.decAlarmCount(productAlarms+linkAlarms, CurrentUser.getWorkspaceId());
					}

					handle.execute("SET FOREIGN_KEY_CHECKS=1");
					handle.commit();
					res = Responses.OK;
				}
			}
		}

		return res;
	}

  /**
   * TODO: testleri yazilmali!!!
   * 
   * Used for one or multiple links
   * 
   * @param dto
   * @return
   */
  Response setAlarmOFF(SetAlarmOFFDTO dto) {
  	if (dto.getAlarmTopic() == null) {
  		return Responses.Invalid.ALARM_TOPIC;
  	}

    Response res = Responses.NotFound.ALARM;

    if (dto.getEntityIdSet() != null && dto.getEntityIdSet().size() > 0) {
      try (Handle handle = Database.getHandle()) {
      	AlarmDao alarmDao = handle.attach(AlarmDao.class);

    		handle.begin();
      	
      	int affected = 0;
      	switch (dto.getAlarmTopic()) {
					case PRODUCT: {
						affected = alarmDao.removeAlarmsByEntityIds("product", dto.getEntityIdSet(), CurrentUser.getWorkspaceId());
						break;
					}
					case LINK: {
						affected = alarmDao.removeAlarmsByEntityIds("link", dto.getEntityIdSet(), CurrentUser.getWorkspaceId());
						break;
					}
				}

      	if (affected > 0) {
	      	if (affected > 0) {
						WorkspaceDao workspaceDao = handle.attach(WorkspaceDao.class);
						workspaceDao.decAlarmCount(affected, CurrentUser.getWorkspaceId());
        		handle.commit();
	      		res = Responses.OK;
	      	} else {
        		handle.rollback();
	      	}
  	    } else {
  	    	res = Responses.NotFound.ALARM;
  	    }
      }
    }

    return res;
  }

	Response search(BaseSearchDTO dto) {
  	dto = DTOHelper.normalizeSearch(dto, true);

		// ---------------------------------------------------
		// building the criteria up
		// ---------------------------------------------------
		StringBuilder where = new StringBuilder();

		where.append("where workspace_id = ");
		where.append(dto.getWorkspaceId());

    if (StringUtils.isNotBlank(dto.getTerm())) {
    	where.append(" and name");
      where.append(" like '%");
      where.append(dto.getTerm());
      where.append("%' ");
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
		        " order by name " + 
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
						|| dto.getAmountLowerLimit().compareTo(new BigDecimal(99_999_999)) > 0);

				boolean hasNoUpperLimit = 
						(dto.getAmountUpperLimit() == null 
						|| dto.getAmountUpperLimit().compareTo(BigDecimal.ZERO) < 1 
						|| dto.getAmountUpperLimit().compareTo(new BigDecimal(99_999_999)) > 0);

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
