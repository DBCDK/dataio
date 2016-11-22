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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * FlowContent DTO class.
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class FlowContent implements Serializable {
    private static final long serialVersionUID = 5520247158829273054L;

    private final String name;
    private final String description;
    private final List<FlowComponent> components;
    private Date timeOfFlowComponentUpdate;

    /**
     * Class constructor
     *
     * @param name flow name
     * @param description flow description
     * @param components flow components attached to this flow (can be empty)
     * @param timeOfFlowComponentUpdate time of last time the flow components nested within the flow were updated
     * @throws NullPointerException if given null-valued name, description or components argument
     * @throws IllegalArgumentException if given empty-valued name
     */
    @JsonCreator
    public FlowContent(@JsonProperty("name") String name,
                       @JsonProperty("description") String description,
                       @JsonProperty("components") List<FlowComponent> components,
                       @JsonProperty("timeOfFlowComponentUpdate") Date timeOfFlowComponentUpdate) {

        this.name = InvariantUtil.checkNotNullNotEmptyOrThrow(name, "name");
        this.description = InvariantUtil.checkNotNullNotEmptyOrThrow(description, "description");
        // We're not making a deep-copy here, but since FlowComponent is immutable
        // (or as near as) this should be sufficient to ensure immutability of this
        // class.
        this.components = new ArrayList<>(InvariantUtil.checkNotNullOrThrow(components, "components"));
        this.timeOfFlowComponentUpdate = timeOfFlowComponentUpdate;
    }

    public FlowContent(String name, String description, List<FlowComponent> components) {
        this(name, description, components, null);
    }

    public List<FlowComponent> getComponents() {
        return new ArrayList<>(components);
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public Date getTimeOfFlowComponentUpdate() {
        return timeOfFlowComponentUpdate;
    }

    public FlowContent withTimeOfFlowComponentUpdate(Date timeOfFlowComponentUpdate) {
        this.timeOfFlowComponentUpdate = new Date(timeOfFlowComponentUpdate.getTime());
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlowContent)) return false;

        FlowContent that = (FlowContent) o;

        return components.equals(that.components)
                && description.equals(that.description)
                && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + components.hashCode();
        return result;
    }
}
