package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.dto.WorkspaceDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.rest.repository.WorkspaceRepository;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

public class AdminWorkspaceService {

    private final WorkspaceRepository repository = Beans.getSingleton(WorkspaceRepository.class);

    public ServiceResponse findById(AuthUser authUser, Long id) {
        return repository.findById(authUser, id);
    }

    public ServiceResponse getList(AuthUser authUser) {
        return repository.getList(authUser);
    }

    public ServiceResponse insert(AuthUser authUser, WorkspaceDTO workspaceDTO) {
        ServiceResponse res = validate(workspaceDTO, true);
        if (res.isOK()) {
            res = repository.insert(authUser, workspaceDTO);
        }
        return res;
    }

    public ServiceResponse update(AuthUser authUser, WorkspaceDTO workspaceDTO) {
        ServiceResponse res = validate(workspaceDTO, false);
        if (res.isOK()) {
            res = repository.update(authUser, workspaceDTO);
        }
        return res;
    }

    public ServiceResponse deleteById(AuthUser authUser, Long id) {
        return repository.deleteById(authUser, id);
    }

    private ServiceResponse validate(WorkspaceDTO workspaceDTO, boolean insert) {
        List<Problem> problems = new ArrayList<>();

        if (! insert && workspaceDTO.getId() == null) {
            problems.add(new Problem("name", "Workspace id cannot be null!"));
        }

        if (StringUtils.isBlank(workspaceDTO.getName())) {
            problems.add(new Problem("name", "Workspace name cannot be null!"));
        } else if (workspaceDTO.getName().length() < 3 || workspaceDTO.getName().length() > 50) {
            problems.add(new Problem("name", "Workspace name must be between 3 and 50 chars!"));
        }

        if (problems.size() > 0) {
            ServiceResponse res = new ServiceResponse(HttpStatus.BAD_REQUEST_400);
            res.setProblems(problems);
            return res;
        } else {
            return InstantResponses.OK;
        }
    }

}
