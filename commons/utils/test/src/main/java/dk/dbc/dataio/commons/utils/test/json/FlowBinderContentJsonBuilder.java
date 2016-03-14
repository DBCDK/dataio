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

package dk.dbc.dataio.commons.utils.test.json;

import dk.dbc.dataio.commons.types.RecordSplitterConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlowBinderContentJsonBuilder extends JsonBuilder {
    private String name = "name";
    private String description = "description";
    private String packaging = "packaging";
    private String format = "format";
    private String charset = "charset";
    private String destination = "destination";
    private String recordSplitter = RecordSplitterConstants.RecordSplitter.XML.name();
    private Long flowId = 42L;
    private List<Long> submitterIds = new ArrayList<>(Collections.singletonList(43L));
    private Long sinkId = 44L;
    private String queueProvider = "queue-provider";

    public FlowBinderContentJsonBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public FlowBinderContentJsonBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public FlowBinderContentJsonBuilder setPackaging(String packaging) {
        this.packaging = packaging;
        return this;
    }

    public FlowBinderContentJsonBuilder setFormat(String format) {
        this.format = format;
        return this;
    }

    public FlowBinderContentJsonBuilder setCharset(String charset) {
        this.charset = charset;
        return this;
    }

    public FlowBinderContentJsonBuilder setDestination(String destination) {
        this.destination = destination;
        return this;
    }

    public FlowBinderContentJsonBuilder setRecordSplitter(RecordSplitterConstants.RecordSplitter recordSplitter) {
        this.recordSplitter = recordSplitter.name();
        return this;
    }

    public FlowBinderContentJsonBuilder setFlowId(Long flowId) {
        this.flowId = flowId;
        return this;
    }

    public FlowBinderContentJsonBuilder setSubmitterIds(List<Long> submitterIds) {
        this.submitterIds = new ArrayList<>(submitterIds);
        return this;
    }

    public FlowBinderContentJsonBuilder setSinkId(Long sinkId) {
        this.sinkId = sinkId;
        return this;
    }

    public FlowBinderContentJsonBuilder setQueueProvider(String queueProvider) {
        this.queueProvider = queueProvider;
        return this;
    }

    public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);
        stringBuilder.append(asTextMember("name", name)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("description", description)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("packaging", packaging)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("format", format)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("charset", charset)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("destination", destination)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("recordSplitter", recordSplitter)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asLongMember("flowId", flowId)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asLongArray("submitterIds", submitterIds)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asLongMember("sinkId", sinkId)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("queueProvider", queueProvider));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}
