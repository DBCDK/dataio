/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;

import java.util.Collections;
import java.util.List;

public class FlowBinderContentBuilder {
    private String name = "flowbinder-name";
    private String description = "flowbinder-description";
    private String packaging = "flowbinder-packaging";
    private String format = "flowbinder-format";
    private String charset = "flowbinder-charset";
    private String destination = "flowbinder-destination";
    private Priority priority = Priority.NORMAL;
    private RecordSplitterConstants.RecordSplitter recordSplitter = RecordSplitterConstants.RecordSplitter.XML;
    private long flowId = 47L;
    private List<Long> submitterIds = Collections.singletonList(78L);
    private long sinkId = 24L;
    private String queueProvider = "queue-provider";


    public FlowBinderContentBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public FlowBinderContentBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public FlowBinderContentBuilder setPackaging(String packaging) {
        this.packaging = packaging;
        return this;
    }

    public FlowBinderContentBuilder setFormat(String format) {
        this.format = format;
        return this;
    }

    public FlowBinderContentBuilder setCharset(String charset) {
        this.charset = charset;
        return this;
    }

    public FlowBinderContentBuilder setDestination(String destination) {
        this.destination = destination;
        return this;
    }

    public FlowBinderContentBuilder setPriority(Priority priority) {
        this.priority = priority;
        return this;
    }

    public FlowBinderContentBuilder setRecordSplitter(RecordSplitterConstants.RecordSplitter recordSplitter) {
        this.recordSplitter = recordSplitter;
        return this;
    }

    public FlowBinderContentBuilder setFlowId(long flowId) {
        this.flowId = flowId;
        return this;
    }

    public FlowBinderContentBuilder setSubmitterIds(List<Long> submitterIds) {
        this.submitterIds = submitterIds;
        return this;
    }

    public FlowBinderContentBuilder setSinkId(long sinkId) {
        this.sinkId = sinkId;
        return this;
    }

    public FlowBinderContentBuilder setQueueProvider(String queueProvider) {
        this.queueProvider = queueProvider;
        return this;
    }

    public FlowBinderContent build() {
        return new FlowBinderContent(name, description, packaging, format, charset, destination, priority, recordSplitter, flowId, submitterIds, sinkId, queueProvider);
    }

}
