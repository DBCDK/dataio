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
import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;

/**
 * SinkContent DTO class.
 */
public class SinkContent implements Serializable {
    private static final long serialVersionUID = -3413557101203220951L;

    public enum SinkType {
        DPF,
        DUMMY,
        ES,
        HIVE,
        HOLDINGS_ITEMS,
        IMS,
        MARCCONV,
        OPENUPDATE,
        PERIODIC_JOBS,
        TICKLE,
        VIP,
        WORLDCAT
    }
    public enum SequenceAnalysisOption { ALL, ID_ONLY }

    private static final SinkType NULL_TYPE = null;
    private static final SinkConfig NULL_CONFIG = null;

    private final String name;
    private final String resource;
    private final String description;
    private final SinkType sinkType;
    private final SinkConfig sinkConfig;
    private final SequenceAnalysisOption sequenceAnalysisOption;

    /**
     * Class constructor
     *
     * @param name sink name
     * @param resource sink resource
     * @param description sink description
     * @param sinkType sink type
     * @param sinkConfig sink config
     * @param sequenceAnalysisOption options for sequence analysis
     *
     * @throws NullPointerException if given null-valued name or resource argument
     * @throws IllegalArgumentException if given empty-valued name or resource argument
     */
    @JsonCreator
    public SinkContent(@JsonProperty("name") String name,
                       @JsonProperty("resource") String resource,
                       @JsonProperty("description") String description,
                       @JsonProperty("sinkType") SinkType sinkType,
                       @JsonProperty("sinkConfig") SinkConfig sinkConfig,
                       @JsonProperty("sequenceAnalysisOption") SequenceAnalysisOption sequenceAnalysisOption) {

        this.name = InvariantUtil.checkNotNullNotEmptyOrThrow(name, "name");
        this.resource = InvariantUtil.checkNotNullNotEmptyOrThrow(resource, "resource");
        this.description = description;
        this.sinkType = sinkType;
        this.sinkConfig = sinkConfig;
        this.sequenceAnalysisOption = InvariantUtil.checkNotNullOrThrow(sequenceAnalysisOption, "sequenceAnalysisOption");
    }

    public SinkContent(String name, String resource, String description, SinkType sinkType, SequenceAnalysisOption sequenceAnalysisOption) {
        this(name, resource, description, sinkType, NULL_CONFIG, sequenceAnalysisOption);
    }

    public SinkContent(String name, String resource, String description, SequenceAnalysisOption sequenceAnalysisOption) {
        this(name, resource, description, NULL_TYPE, NULL_CONFIG, sequenceAnalysisOption);
    }

    public String getName() {
        return name;
    }

    public String getResource() {
        return resource;
    }

    public String getDescription() {
        return description;
    }

    public SinkType getSinkType() {
        return sinkType;
    }

    public SinkConfig getSinkConfig() {
        return sinkConfig;
    }

    public SequenceAnalysisOption getSequenceAnalysisOption() {
        return sequenceAnalysisOption;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SinkContent)) return false;

        SinkContent that = (SinkContent) o;

        if (!name.equals(that.name)) return false;
        if (!resource.equals(that.resource)) return false;
        if (!description.equals(that.description)) return false;
        if (sinkType != that.sinkType) return false;
        if (sinkConfig != null ? !sinkConfig.equals(that.sinkConfig) : that.sinkConfig != null) return false;
        return sequenceAnalysisOption == that.sequenceAnalysisOption;

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + resource.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + (sinkType != null ? sinkType.hashCode() : 0);
        result = 31 * result + (sinkConfig != null ? sinkConfig.hashCode() : 0);
        result = 31 * result + sequenceAnalysisOption.hashCode();
        return result;
    }
}