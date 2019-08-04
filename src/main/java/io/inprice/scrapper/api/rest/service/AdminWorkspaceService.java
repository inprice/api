package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.dto.WorkspaceDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.Claims;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.Response;
import io.inprice.scrapper.api.info.Responses;
import io.inprice.scrapper.api.rest.repository.WorkspaceRepository;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

public class AdminWorkspaceService {

    private final WorkspaceRepository repository = Beans.getSingleton(WorkspaceRepository.class);

    public Response findById(Claims claims, Long id) {
        return repository.findById(claims, id);
    }

    public Response getList(long companyId) {
        return repository.getList(companyId);
    }

    public Response insert(Claims claims, WorkspaceDTO workspaceDTO) {
        Response res = validate(workspaceDTO, true);
        if (res.isOK()) {
            res = repository.insert(claims.getCompanyId(), workspaceDTO);
        }
        return res;
    }

    public Response update(Claims claims, WorkspaceDTO workspaceDTO, boolean passwordWillBeUpdated) {
        Response res = validate(workspaceDTO, false);
        if (res.isOK()) {
            res = repository.update(workspaceDTO);
        }
        return res;
    }

    public Response deleteById(Long id) {
        return repository.deleteById(id);
    }

    private Response validate(WorkspaceDTO workspaceDTO, boolean insert) {
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
            Response res = new Response(HttpStatus.BAD_REQUEST_400);
            res.setProblems(problems);
            return res;
        } else {
            return Responses.OK;
        }
    }

}
