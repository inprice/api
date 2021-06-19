package io.inprice.api.app.alarm;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.alarm.dto.AlarmDTO;
import io.inprice.api.app.alarm.dto.OrderBy;
import io.inprice.api.app.alarm.dto.SearchDTO;
import io.inprice.api.app.group.GroupDao;
import io.inprice.api.app.link.LinkDao;
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
import io.inprice.common.models.Alarm;
import io.inprice.common.models.Link;
import io.inprice.common.models.LinkGroup;

/**
 * 
 * @since 2021-06-16
 * @author mdpinar
 */
public class AlarmService {

	private static final Logger log = LoggerFactory.getLogger(AlarmService.class);

	Response insert(AlarmDTO dto) {
		Response res = Responses.Invalid.ALARM;

		if (dto != null) {
			String problem = validate(dto);
			if (problem == null) {
				try (Handle handle = Database.getHandle()) {
					Pair<String, BigDecimal> pair = findStatusAndPrice(dto, handle);

					if (pair != null) {
						AlarmDao alarmDao = handle.attach(AlarmDao.class);
						handle.begin();
						long id = alarmDao.insert(dto, pair);

						boolean isOK = false;
						if (AlarmTopic.LINK.equals(dto.getTopic())) {
							isOK = alarmDao.setAlarmForLink(dto.getLinkId(), id, CurrentUser.getAccountId());
						} else {
							isOK = alarmDao.setAlarmForGroup(dto.getGroupId(), id, CurrentUser.getAccountId());
						}

						if (isOK) {
							handle.commit();
							dto.setId(id);
							res = new Response(dto);
						} else {
							handle.rollback();
							res = Responses.DataProblem.DB_PROBLEM;
						}
					}
				}
			} else {
				res = new Response(problem);
			}
		}
		return res;
	}

	Response update(AlarmDTO dto) {
		Response res = Responses.NotFound.ALARM;

		if (dto != null && dto.getId() != null && dto.getId() > 0) {
			String problem = validate(dto);
			if (problem == null) {
				try (Handle handle = Database.getHandle()) {
					Pair<String, BigDecimal> pair = findStatusAndPrice(dto, handle);

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

				Alarm alarm = alarmDao.findById(id, CurrentUser.getAccountId());
				if (alarm != null) {

					handle.begin();
					handle.execute("SET FOREIGN_KEY_CHECKS=0");

					boolean isOK = false;
					if (AlarmTopic.LINK.equals(alarm.getTopic())) {
						isOK = alarmDao.removeAlarmFromLink(alarm.getLinkId(), CurrentUser.getAccountId());
					} else {
						isOK = alarmDao.removeAlarmFromGroup(alarm.getGroupId(), CurrentUser.getAccountId());
					}

					if (isOK) {
						isOK = alarmDao.delete(id, CurrentUser.getAccountId());
						if (isOK) {
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

	public Response search(SearchDTO dto) {
		if (dto.getTerm() != null)
			dto.setTerm(SqlHelper.clear(dto.getTerm()));

		// ---------------------------------------------------
		// building the criteria up
		// ---------------------------------------------------
		StringBuilder crit = new StringBuilder();

		crit.append("where a.account_id = ");
		crit.append(CurrentUser.getAccountId());

		if (dto.getSubjects() != null && dto.getSubjects().size() > 0) {
			crit.append(
			    String.format(" and subject in (%s) ", io.inprice.common.utils.StringUtils.join("'", dto.getSubjects())));
		}

		if (dto.getWhens() != null && dto.getWhens().size() > 0) {
			crit.append(
			    String.format(" and subject_when in (%s) ", io.inprice.common.utils.StringUtils.join("'", dto.getWhens())));
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

		String select = 
				"select a.*, g.name as group_name, l.name as link_name, l.url as link_url from alarm a "
		    + "left join link_group g on g.id = a.group_id " + generateNameLikeClause(dto, "g")
		    + "left join link l on l.id = a.link_id " + generateNameLikeClause(dto, "l");
		
		if (dto.getTopic() != null) {
			if (AlarmTopic.LINK.equals(dto.getTopic())) {
				select = 
						"select a.*, '' as group_name, l.name as link_name, l.url as link_url from alarm a "
				    + "inner join link l on l.id = a.link_id " + generateNameLikeClause(dto, "l");
			} else if (AlarmTopic.GROUP.equals(dto.getTopic())) {
				select = 
						"select a.*, g.name as group_name, '' as link_name, '' as link_url from alarm a "
				    + "inner join link_group g on g.id = a.group_id " + generateNameLikeClause(dto, "g");
			}
		}

		String orderBy = " order by " + dto.getOrderBy().getFieldName() + dto.getOrderDir().getDir();
		if (OrderBy.NAME.equals(dto.getOrderBy()) && AlarmTopic.LINK.equals(dto.getTopic())) {
			orderBy = " order by l.name, l.url" + dto.getOrderDir().getDir();
		}
		
		try (Handle handle = Database.getHandle()) {
			List<Alarm> searchResult = 
				handle
			    .createQuery(
		        select + 
		        crit + 
		        orderBy + 
		        limit
	        )
		    .map(new AlarmMapper()).list();

			return new Response(Collections.singletonMap("rows", searchResult));
		} catch (Exception e) {
			log.error("Failed in full search for alarms.", e);
			return Responses.ServerProblem.EXCEPTION;
		}
	}

	private String generateNameLikeClause(SearchDTO dto, String prefix) {
		StringBuilder sb = new StringBuilder();
		if (StringUtils.isNotBlank(dto.getTerm())) {
			sb.append(" and ");
			sb.append(prefix);
			sb.append(".name like '%");
			sb.append(dto.getTerm());
			sb.append("%' ");
			if ("l".equals(prefix)) {
				sb.append(" and ");
				sb.append(prefix);
				sb.append(".url like '%");
				sb.append(dto.getTerm());
				sb.append("%' ");
			}
		}
		return sb.toString();
	}

	private String validate(AlarmDTO dto) {
		String problem = null;

		if (dto.getTopic() == null) {
			problem = "Topic cannot be empty!";
		}

		if (problem == null && dto.getGroupId() == null && dto.getLinkId() == null) {
			problem = "Topic id cannot be empty!";
		}

		if (problem == null && dto.getSubject() == null) {
			problem = "Subject cannot be empty!";
		}

		if (problem == null && dto.getSubjectWhen() == null) {
			problem = "You are expected to specify of when the subject should be considered!";
		}

		if (problem == null && dto.getSubject() == null) {
			if (AlarmSubject.STATUS.equals(dto.getSubject()) && !AlarmSubjectWhen.CHANGED.equals(dto.getSubjectWhen())) {
				if (StringUtils.isBlank(dto.getCertainStatus())) {
					problem = "You are expected to specify certain status!";
				}
			} else {
				dto.setCertainStatus(null);
			}
		}

		if (problem == null && dto.getSubject() == null) {
			if (!AlarmSubject.STATUS.equals(dto.getSubject())
			    && AlarmSubjectWhen.OUT_OF_LIMITS.equals(dto.getSubjectWhen())) {
				boolean hasNoLowerLimit = (dto.getPriceLowerLimit() == null
				    || dto.getPriceLowerLimit().compareTo(BigDecimal.ZERO) < 1);
				boolean hasNoUpperLimit = (dto.getPriceUpperLimit() == null
				    || dto.getPriceUpperLimit().compareTo(BigDecimal.ZERO) < 1);
				if (hasNoLowerLimit && hasNoUpperLimit) {
					problem = "You are expected to specify either lower or upper limit for "
					    + AlarmSubject.STATUS.name().toLowerCase();
				} else {
					if (hasNoLowerLimit)
						dto.setPriceLowerLimit(BigDecimal.ZERO);
					if (hasNoUpperLimit)
						dto.setPriceUpperLimit(BigDecimal.ZERO);
				}
			} else {
				dto.setPriceLowerLimit(BigDecimal.ZERO);
				dto.setPriceUpperLimit(BigDecimal.ZERO);
			}
		}

		if (problem == null) {
			dto.setAccountId(CurrentUser.getAccountId());
		}

		return problem;
	}

	private Pair<String, BigDecimal> findStatusAndPrice(AlarmDTO dto, Handle handle) {
		Pair<String, BigDecimal> pair = null;
		
		switch (dto.getTopic()) {
			case LINK: {
  			LinkDao linkDao = handle.attach(LinkDao.class);
  			Link link = linkDao.findById(dto.getLinkId(), CurrentUser.getAccountId());
  			if (link != null) {
  				pair = new Pair<>();
  				pair.setLeft(link.getStatusGroup().name());
  				pair.setRight(link.getPrice());
  			}
  			break;
			}

			case GROUP: {
  			GroupDao groupDao = handle.attach(GroupDao.class);
  			LinkGroup group = groupDao.findById(dto.getGroupId(), CurrentUser.getAccountId());
  			if (group != null) {
  				pair = new Pair<>();
  				pair.setLeft(group.getLevel().name());
  				switch (dto.getSubject()) {
  				case MINIMUM: {
  					pair.setRight(group.getMinPrice());
  					break;
  				}
  				case AVERAGE: {
  					pair.setRight(group.getAvgPrice());
  					break;
  				}
  				case MAXIMUM: {
  					pair.setRight(group.getMaxPrice());
  					break;
  				}
  				case TOTAL: {
  					pair.setRight(group.getTotal());
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
