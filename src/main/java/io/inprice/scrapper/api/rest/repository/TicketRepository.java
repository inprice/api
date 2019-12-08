package io.inprice.scrapper.api.rest.repository;

import io.inprice.scrapper.api.dto.TicketDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.common.meta.TicketSource;
import io.inprice.scrapper.common.meta.TicketStatus;
import io.inprice.scrapper.common.meta.TicketType;
import io.inprice.scrapper.common.meta.Role;
import io.inprice.scrapper.common.models.Ticket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class TicketRepository {

    private static final Logger log = LoggerFactory.getLogger(TicketRepository.class);
    private final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);

    public ServiceResponse<Ticket> findById(Long id) {
        Ticket model = dbUtils.findSingle(
            String.format(
            "select * from ticket " +
                "where id = %d " +
                "  and reported_by = %d ", id, Context.getUserId()), this::map);
        if (model != null) {
            return new ServiceResponse<>(model);
        }
        return Responses.NotFound.TICKET;
    }

    public ServiceResponse<Ticket> getList(TicketSource source, Long id) {
        List<Ticket> tickets = dbUtils.findMultiple(
            String.format(
                "select * from ticket " +
                "where %s = %d " +
                "order by created_at", findField(source), id
            ), this::map);

        if (tickets != null && tickets.size() > 0) {
            return new ServiceResponse<>(tickets);
        }
        return Responses.NotFound.TICKET;
    }

    public ServiceResponse insert(TicketDTO ticketDTO) {
        Connection con = null;
        try {
            con = dbUtils.getTransactionalConnection();

            boolean result = false;
            boolean sourceFound;
            String wherePart = "";

            if (TicketSource.COMPANY.equals(ticketDTO.getSource())) {
                if (! ticketDTO.getSourceId().equals(Context.getCompanyId())) {
                    return Responses.NotFound.COMPANY;
                }
            } else {
                wherePart = "and company_id="+Context.getCompanyId();
            }

            try (PreparedStatement
                 pst = con.prepareStatement(
                    String.format(
                    "select id from %s where id=%d " + wherePart,
                        ticketDTO.getSource().name().toLowerCase(), ticketDTO.getSourceId()));
                ResultSet rs = pst.executeQuery()) {
                sourceFound = (rs.next());
            }

            if (sourceFound) {
                final String field = findField(ticketDTO.getSource());

                final String query =
                    "insert into ticket " +
                    "(source, ticket_type, description, reported_by, " + field + ") " +
                    " values " +
                    "(?, ?, ?, ?, ?) ";

                try (PreparedStatement pst = con.prepareStatement(query)) {
                    int i = 0;
                    pst.setString(++i, ticketDTO.getSource().name());
                    pst.setString(++i, ticketDTO.getType().name());
                    pst.setString(++i, ticketDTO.getDescription());
                    pst.setLong(++i, Context.getUserId());
                    pst.setLong(++i, ticketDTO.getSourceId());

                    result = (pst.executeUpdate() > 0);
                }
            } else {
                switch (ticketDTO.getSource()) {
                    case PRODUCT: return Responses.NotFound.PRODUCT;
                    case LINK: return Responses.NotFound.LINK;
                    case COMPANY: return Responses.NotFound.COMPANY;
                    case WORKSPACE: return Responses.NotFound.WORKSPACE;
                }
            }

            if (result) {
                dbUtils.commit(con);
                return Responses.OK;
            } else {
                dbUtils.rollback(con);
                return Responses.DataProblem.DB_PROBLEM;
            }
        } catch (SQLException e) {
            if (con != null) dbUtils.rollback(con);
            log.error("Failed to insert a new ticket" + ticketDTO.toString(), e);
            return Responses.ServerProblem.EXCEPTION;
        } finally {
            if (con != null) dbUtils.close(con);
        }
    }

    public ServiceResponse update(TicketDTO ticketDTO) {
        final String wherePart = (Role.admin.equals(Context.getAuthUser().getRole()) ? "" : "and reported_by="+Context.getUserId());

        try (Connection con = dbUtils.getConnection();
             PreparedStatement pst = con.prepareStatement("update ticket set description=? where id=? " + wherePart)) {

            int i = 0;
            pst.setString(++i, ticketDTO.getDescription());
            pst.setLong(++i, ticketDTO.getId());

            if (pst.executeUpdate() > 0)
                return Responses.OK;
            else
                return Responses.NotFound.TICKET;

        } catch (SQLException sqle) {
            log.error("Failed to update ticket", sqle);
            return Responses.ServerProblem.EXCEPTION;
        }
    }

    public ServiceResponse deleteById(Long id) {
        final String wherePart = (Role.admin.equals(Context.getAuthUser().getRole()) ? "" : "and reported_by="+Context.getUserId());

        boolean result = dbUtils.executeQuery(
            String.format(
                "delete from ticket where id=%d and status='%s' " + wherePart,
                id, TicketStatus.NEW
            ),"Failed to delete ticket. Id: " + id
        );

        if (result) {
            return Responses.OK;
        }
        return Responses.NotFound.TICKET;
    }

    private String findField(TicketSource source) {
        return source.name().toLowerCase() + "_id";
    }

    private Ticket map(ResultSet rs) {
        try {
            Ticket model = new Ticket();
            model.setId(rs.getLong("id"));
            model.setStatus(TicketStatus.valueOf(rs.getString("status")));
            model.setSource(TicketSource.valueOf(rs.getString("source")));
            model.setType(TicketType.valueOf(rs.getString("ticket_type")));
            model.setDescription(rs.getString("description"));
            model.setLinkId(rs.getLong("link_id"));
            model.setProductId(rs.getLong("product_id"));
            model.setWorkspaceId(rs.getLong("workspace_id"));
            model.setCompanyId(rs.getLong("company_id"));
            model.setReportedBy(rs.getLong("reported_by"));
            model.setCreatedAt(rs.getDate("created_at"));

            return model;
        } catch (SQLException e) {
            log.error("Failed to set ticket's properties", e);
        }
        return null;
    }

}
