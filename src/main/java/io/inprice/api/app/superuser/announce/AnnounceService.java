package io.inprice.api.app.superuser.announce;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;

import io.inprice.api.app.superuser.announce.dto.AnnounceDTO;
import io.inprice.api.consts.Responses;
import io.inprice.api.info.Response;
import io.inprice.common.helpers.Database;

/**
 * 
 * @since 2021-06-05
 * @author mdpinar
*/
public class AnnounceService {

	Response insert(AnnounceDTO dto) {
		Response res = Responses.Invalid.ANNOUNCE;

		if (dto != null) {
			String problem = validate(dto);
			if (problem == null) {
				try (Handle handle = Database.getHandle()) {
					AnnounceDao announceDao = handle.attach(AnnounceDao.class);

					boolean isOK = announceDao.insert(dto);
					if (isOK) {
						res = Responses.OK;
					} else {
						res = Responses.DataProblem.DB_PROBLEM;
					}
				}
			} else {
				res = new Response(problem);
			}
		}
		return res;
	}

	Response update(AnnounceDTO dto) {
		Response res = Responses.Invalid.TICKET;

		if (dto != null && dto.getId() != null && dto.getId() > 0) {
			String problem = validate(dto);
			if (problem == null) {
				try (Handle handle = Database.getHandle()) {
					AnnounceDao announceDao = handle.attach(AnnounceDao.class);

					boolean isOK = announceDao.update(dto);
					if (isOK) {
						res = Responses.OK;
					} else {
						res = Responses.DataProblem.DB_PROBLEM;
					}
				}
			} else {
				res = new Response(problem);
			}
		}
		return res;
	}
	
	Response delete(Long id) {
		Response res = Responses.NotFound.TICKET;

		if (id != null && id > 0) {
			try (Handle handle = Database.getHandle()) {
				AnnounceDao announceDao = handle.attach(AnnounceDao.class);

				boolean isOK = announceDao.delete(id);
				if (isOK) {
					res = Responses.OK;
				} else {
					handle.rollback();
					res = Responses.DataProblem.DB_PROBLEM;
				}
			}
		}

		return res;
	}
	
	private String validate(AnnounceDTO dto) {
		String problem = null;
		
		if (StringUtils.isBlank(dto.getTitle())) {
			problem = "Title cannot be empty!";
		} else if (dto.getTitle().length() < 3 || dto.getTitle().length() > 50) {
			problem = "Title must be between 3-50 chars!";
		}

		if (problem == null) {
  		if (StringUtils.isBlank(dto.getContent())) {
  			problem = "Content cannot be empty!";
  		} else if (dto.getContent().length() < 12 || dto.getContent().length() > 1024) {
  			problem = "Content must be between 12-1024 chars!";
  		}
		}

		if (problem == null && dto.getType() == null) {
			problem = "Type cannot be empty!";
		}

		if (problem == null && dto.getLevel() == null) {
			problem = "Level cannot be empty!";
		}

		return problem;
	}

}
