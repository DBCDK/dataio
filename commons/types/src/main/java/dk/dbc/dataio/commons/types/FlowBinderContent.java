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

package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * FlowBinderContent DTO class.
 */
public class FlowBinderContent implements Serializable {
    private static final long serialVersionUID = 1106844598199379043L;

    private final String name;
    private final String description;
    private final String packaging;
    private final String format;
    private final String charset;
    private final String destination;
    private final RecordSplitterConstants.RecordSplitter recordSplitter;
    private final boolean sequenceAnalysis;
    private final long flowId;
    private final List<Long> submitterIds;
    private final long sinkId;
    private final String queueProvider;

    /**
     * Class constructor
     *
     * @param name flowbinder name
     * @param description flowbinder description
     * @param packaging flowbinder packaging (rammeformat)
     * @param format flowbinder format (indholdsformat)
     * @param charset flowbinder character set
     * @param destination flow binder destination
     * @param recordSplitter flow binder record splitter
     * @param sequenceAnalysis boolean for telling whether sequence analysis is on or off for the flowbinder.
     * @param flowId id of flow attached to this flowbinder
     * @param submitterIds ids of submitters attached to this flowbinder
     * @param sinkId id of sink attached to this flowbinder
     * @param queueProvider the queue provider to use for this flow binder
     *
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty valued String or List argument
     */

    @JsonCreator
    public FlowBinderContent(@JsonProperty("name") String name,
                             @JsonProperty("description") String description,
                             @JsonProperty("packaging") String packaging,
                             @JsonProperty("format") String format,
                             @JsonProperty("charset") String charset,
                             @JsonProperty("destination") String destination,
                             @JsonProperty("recordSplitter") RecordSplitterConstants.RecordSplitter recordSplitter,
                             @JsonProperty("sequenceAnalysis") boolean sequenceAnalysis, // TODO: 04/03/16 Should be removed once the objects stored in flowstore have been modified
                             @JsonProperty("flowId") long flowId,
                             @JsonProperty("submitterIds") List<Long> submitterIds,
                             @JsonProperty("sinkId") long sinkId,
                             @JsonProperty("queueProvider") String queueProvider) {

        this.name = InvariantUtil.checkNotNullNotEmptyOrThrow(name, "name");
        this.description = InvariantUtil.checkNotNullNotEmptyOrThrow(description, "description");
        this.packaging = InvariantUtil.checkNotNullNotEmptyOrThrow(packaging, "packaging");
        this.format = InvariantUtil.checkNotNullNotEmptyOrThrow(format, "format");
        this.charset = InvariantUtil.checkNotNullNotEmptyOrThrow(charset, "charset");
        this.destination = InvariantUtil.checkNotNullNotEmptyOrThrow(destination, "destination");
        this.recordSplitter = InvariantUtil.checkNotNullOrThrow(recordSplitter, "recordSplitter");
        this.sequenceAnalysis = sequenceAnalysis;
        this.flowId = InvariantUtil.checkLowerBoundOrThrow(flowId, "flowId", Constants.PERSISTENCE_ID_LOWER_BOUND);
        this.submitterIds = new ArrayList<>(InvariantUtil.checkNotNullOrThrow(submitterIds, "submitterIds"));
        if (this.submitterIds.size() == 0) {
            throw new IllegalArgumentException("submitterIds can not be empty");
        }
        this.sinkId = InvariantUtil.checkLowerBoundOrThrow(sinkId, "sinkId", Constants.PERSISTENCE_ID_LOWER_BOUND);
        this.queueProvider = queueProvider;  // No invariant check due to backwards compatibility issues
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPackaging() {
        return packaging;
    }

    public String getFormat() {
        return format;
    }

    public String getCharset() {
        return charset;
    }

    public String getDestination() {
        return destination;
    }

    public RecordSplitterConstants.RecordSplitter getRecordSplitter() {
        return recordSplitter;
    }

    public boolean getSequenceAnalysis() {
        return sequenceAnalysis;
    }

    public long getFlowId() {
        return flowId;
    }

    public List<Long> getSubmitterIds() {
        return new ArrayList<>(submitterIds);
    }

    public long getSinkId() {
        return sinkId;
    }

    public String getQueueProvider() {
        return queueProvider;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlowBinderContent)) return false;

        FlowBinderContent that = (FlowBinderContent) o;

        return sequenceAnalysis == that.sequenceAnalysis
                && flowId == that.flowId
                && sinkId == that.sinkId
                && name.equals(that.name)
                && description.equals(that.description)
                && packaging.equals(that.packaging)
                && format.equals(that.format)
                && charset.equals(that.charset)
                && destination.equals(that.destination)
                && recordSplitter == that.recordSplitter
                && submitterIds.equals(that.submitterIds)
                && queueProvider.equals(that.queueProvider);
    }


    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + packaging.hashCode();
        result = 31 * result + format.hashCode();
        result = 31 * result + charset.hashCode();
        result = 31 * result + destination.hashCode();
        result = 31 * result + recordSplitter.hashCode();
        result = 31 * result + (sequenceAnalysis ? 1 : 0);
        result = 31 * result + (int) (flowId ^ (flowId >>> 32));
        result = 31 * result + submitterIds.hashCode();
        result = 31 * result + (int) (sinkId ^ (sinkId >>> 32));
        result = 31 * result + (queueProvider != null ? queueProvider.hashCode() : 0);
        return result;
    }
}
