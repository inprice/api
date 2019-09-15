package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.dto.WorkspaceDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Commons;
import io.inprice.scrapper.api.rest.repository.WorkspaceRepository;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class AdminWorkspaceService {

    private static final WorkspaceRepository repository = Beans.getSingleton(WorkspaceRepository.class);

    public ServiceResponse findById(Long id) {
        return repository.findById(id);
    }

    public ServiceResponse getList() {
        return repository.getList();
    }

    public ServiceResponse insert(WorkspaceDTO workspaceDTO) {
        if (workspaceDTO != null) {
            ServiceResponse res = validate(workspaceDTO);
            if (res.isOK()) {
                res = repository.insert(workspaceDTO);
            }
            return res;
        }
        return Responses.Invalid.WORKSPACE;
    }

    public ServiceResponse update(WorkspaceDTO workspaceDTO) {
        if (workspaceDTO != null) {
            if (workspaceDTO.getId() == null || workspaceDTO.getId() < 1) {
                return Responses.NotFound.WORKSPACE;
            }

            ServiceResponse res = validate(workspaceDTO);
            if (res.isOK()) {
                res = repository.update(workspaceDTO);
            }
            return res;
        }
        return Responses.Invalid.WORKSPACE;
    }

    public ServiceResponse deleteById(Long id) {
        if (id == null || id < 1) return Responses.NotFound.WORKSPACE;
        return repository.deleteById(id);
    }

    private ServiceResponse validate(WorkspaceDTO workspaceDTO) {
        List<Problem> problems = new ArrayList<>();

        if (StringUtils.isBlank(workspaceDTO.getName())) {
            problems.add(new io.inprice.scrapper.api.info.Problem("name", "Workspace name cannot be null!"));
        } else if (workspaceDTO.getName().length() < 3 || workspaceDTO.getName().length() > 50) {
            problems.add(new io.inprice.scrapper.api.info.Problem("name", "Workspace name must be between 3 and 50 chars!"));
        }

        return Commons.createResponse(problems);
    }

}
