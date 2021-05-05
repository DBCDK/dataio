/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.flowstore.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlowBinderContentMatch {
    private static final JSONBContext JSONB_CONTEXT = new JSONBContext();

    private String charset;
    private String destination;
    private String format;
    private String packaging;
    private List<Long> submitterIds;

    public String getCharset() {
        return charset;
    }

    public FlowBinderContentMatch withCharset(String charset) {
        this.charset = charset;
        return this;
    }

    public String getDestination() {
        return destination;
    }

    public FlowBinderContentMatch withDestination(String destination) {
        this.destination = destination;
        return this;
    }

    public String getFormat() {
        return format;
    }

    public FlowBinderContentMatch withFormat(String format) {
        this.format = format;
        return this;
    }

    public String getPackaging() {
        return packaging;
    }

    public FlowBinderContentMatch withPackaging(String packaging) {
        this.packaging = packaging;
        return this;
    }

    public List<Long> getSubmitterIds() {
        return submitterIds;
    }

    public FlowBinderContentMatch withSubmitterIds(List<Long> submitterIds) {
        this.submitterIds = submitterIds;
        return this;
    }

    @Override
    public String toString() {
        try {
            return JSONB_CONTEXT.marshall(this);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
