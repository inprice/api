package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.dto.TicketDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Commons;
import io.inprice.scrapper.api.rest.repository.TicketRepository;
import io.inprice.scrapper.common.meta.TicketSource;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class TicketService {

    private final TicketRepository repository = Beans.getSingleton(TicketRepository.class);

    public ServiceResponse findById(Long id) {
        return repository.findById(id);
    }

    public ServiceResponse getList(TicketSource source, Long id) {
        return repository.getList(source, id);
    }

    public ServiceResponse insert(TicketDTO ticketDTO) {
        if (ticketDTO != null) {
            ServiceResponse res = validate(ticketDTO, false);
            if (res.isOK()) {
                res = repository.insert(ticketDTO);
            }
            return res;
        }
        return Responses.Invalid.TICKET;
    }

    public ServiceResponse update(TicketDTO ticketDTO) {
        if (ticketDTO != null) {
            if (ticketDTO.getId() == null || ticketDTO.getId() < 1) {
                return Responses.NotFound.TICKET;
            }

            ServiceResponse res = validate(ticketDTO, true);
            if (res.isOK()) {
                res = repository.update(ticketDTO);
            }
            return res;
        }
        return Responses.Invalid.TICKET;
    }

    public ServiceResponse deleteById(Long id) {
        if (id == null || id < 1) return Responses.Invalid.TICKET;
        return repository.deleteById(id);
    }

    private ServiceResponse validate(TicketDTO ticketDTO, boolean isUpdate) {
        List<String> problems = new ArrayList<>();

        if (StringUtils.isBlank(ticketDTO.getDescription())) {
            problems.add("Description cannot be null!");
        } else if (ticketDTO.getDescription().length() < 5 || ticketDTO.getDescription().length() > 255) {
            problems.add("Description must be between 5 and 255 chars!");
        }

        if (! isUpdate) {
            if (ticketDTO.getSource() == null) {
                problems.add("Source cannot be null!");
            }

            if (ticketDTO.getType() == null) {
                problems.add("Type cannot be null!");
            }

            if (ticketDTO.getSourceId() == null) {
                problems.add("Unknown source id!");
            }
        }

        return Commons.createResponse(problems);
    }

}
