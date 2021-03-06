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

package dk.dbc.dataio.gui.client.modelBuilders;

import dk.dbc.dataio.commons.types.SinkConfig;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.model.SinkModel;

/**
 * This class is the Builder class for SinkModel's
 */
public class SinkModelBuilder {
    private long id = 64L;
    private long version = 1L;
    private SinkContent.SinkType sinkType = SinkContent.SinkType.DUMMY;
    private String name = "name";
    private String resource = "resource";
    private String description = "description";
    private SinkContent.SequenceAnalysisOption sequenceAnalysisOption = SinkContent.SequenceAnalysisOption.ALL;
    private SinkConfig sinkConfig = null;

    /**
     * Sets the ID for the Sink
     * @param id Id
     * @return The SinkModelBuilder object itself (for chaining)
     */
    public SinkModelBuilder setId(long id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the Version for the Sink
     * @param version Version
     * @return The SinkModelBuilder object itself (for chaining)
     */
    public SinkModelBuilder setVersion(long version) {
        this.version = version;
        return this;
    }

    /**
     * Sets the SinkType for the Sink
     * @param sinkType Sink Type
     * @return The SinkModelBuilder object itself (for chaining)
     */
    public SinkModelBuilder setSinkType(SinkContent.SinkType sinkType) {
        this.sinkType = sinkType;
        return this;
    }

    /**
     * Sets the Name of the Sink
     * @param name Name of the Sink
     * @return The SinkModelBuilder object itself (for chaining)
     */
    public SinkModelBuilder setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the Resource of the Sink
     * @param resource Resource
     * @return The SinkModelBuilder object itself (for chaining)
     */
    public SinkModelBuilder setResource(String resource) {
        this.resource = resource;
        return this;
    }

    /**
     * Sets the Description of the Sink
     * @param description Description
     * @return The SinkModelBuilder object itself (for chaining)
     */
    public SinkModelBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Sets the SequenceAnalysisOption for the Sink
     * @param sequenceAnalysisOption Sequence Analysis Option
     * @return The SinkModelBuilder object itself (for chaining)
     */
    public SinkModelBuilder setSequenceAnalysisOption(SinkContent.SequenceAnalysisOption sequenceAnalysisOption) {
        this.sequenceAnalysisOption = sequenceAnalysisOption;
        return this;
    }

    /**
     * Sets the SinkConfig for the Sink
     * @param sinkConfig Sink Config
     * @return The SinkModelBuilder object itself (for chaining)
     */
    public SinkModelBuilder setSinkConfig(SinkConfig sinkConfig) {
        this.sinkConfig = sinkConfig;
        return this;
    }

    /**
     * Build the SinkModel object
     * @return The SinkModel object
     */
    public SinkModel build() {
        return new SinkModel(id, version, sinkType, name, resource, description, sequenceAnalysisOption, sinkConfig);
    }
}
