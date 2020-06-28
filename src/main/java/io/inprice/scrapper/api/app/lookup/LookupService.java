package io.inprice.scrapper.api.app.lookup;

import org.apache.commons.lang3.StringUtils;

import io.inprice.scrapper.api.dto.LookupDTO;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.meta.LookupType;

public class LookupService {

  private final LookupRepository repository = Beans.getSingleton(LookupRepository.class);

  public ServiceResponse add(LookupDTO dto) {
    String problem = null;

    dto.setType(dto.getType().trim().toUpperCase());
    dto.setNewValue(dto.getNewValue().trim());

    try {
      LookupType.valueOf(dto.getType()); //just checking
    } catch (Exception e) {
      problem = "Invalid type!";
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getNewValue())) {
        problem = "Value cannot be empty!";
      } else {
        if (dto.getNewValue().length() < 1 || dto.getNewValue().length() > 50) {
          problem = "Value must be 1-50 chars!";
        }
      }
    }

    if (problem == null) {
      return repository.add(dto);
    }

    return new ServiceResponse(problem);
  }

  public ServiceResponse getList(LookupType type) {
    return repository.getList(null, type);
  }

}
