package io.inprice.scrapper.api.dto;

import io.inprice.scrapper.common.meta.TicketSource;
import io.inprice.scrapper.common.meta.TicketType;

public class TicketDTO {

    private Long id;
    private TicketSource source;
    private TicketType type;
    private String description;
    private Long sourceId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TicketSource getSource() {
        return source;
    }

    public void setSource(TicketSource source) {
        this.source = source;
    }

    public TicketType getType() {
        return type;
    }

    public void setType(TicketType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    @Override
    public String toString() {
        return "TicketDTO{" +
                "source=" + source +
                ", type=" + type +
                ", description='" + description + '\'' +
                ", sourceId=" + sourceId +
                '}';
    }
}
