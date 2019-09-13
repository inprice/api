package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.dto.WorkspaceDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.controller.AdminController;
import io.inprice.scrapper.api.rest.repository.WorkspaceRepository;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AdminWorkspaceService {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final WorkspaceRepository repository = Beans.getSingleton(WorkspaceRepository.class);

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
        return InstantResponses.INVALID_DATA("workspace data!");
    }

    public ServiceResponse update(WorkspaceDTO workspaceDTO) {
        if (workspaceDTO.getId() == null || workspaceDTO.getId() < 1) {
            return InstantResponses.NOT_FOUND("Workspace");
        }

        ServiceResponse res = validate(workspaceDTO);
        if (res.isOK()) {
            res = repository.update(workspaceDTO);
        }
        return res;
    }

    public ServiceResponse deleteById(Long id) {
        if (id == null || id < 1) {
            return InstantResponses.NOT_FOUND("Workspace");
        }

        return repository.deleteById(id);
    }

    private ServiceResponse validate(WorkspaceDTO workspaceDTO) {
        List<Problem> problems = new ArrayList<>();

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
