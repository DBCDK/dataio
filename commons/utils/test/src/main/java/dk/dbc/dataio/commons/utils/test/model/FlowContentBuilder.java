package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowContent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class FlowContentBuilder {
    private String name = "name";
    private String description = "description";
    private String entrypointScript = "entrypointScript";
    private String entrypointFunction = "entrypointFunction";
    private byte[] jsar = null;
    private Date timeOfLastModification = null;

    private List<FlowComponent> components = new ArrayList<>(Collections.singletonList(
            new FlowComponentBuilder().build()));
    private Date timeOfFlowComponentUpdate = null;

    public FlowContentBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public FlowContentBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public FlowContentBuilder setEntrypointScript(String entrypointScript) {
        this.entrypointScript = entrypointScript;
        return this;
    }

    public FlowContentBuilder setEntrypointFunction(String entrypointFunction) {
        this.entrypointFunction = entrypointFunction;
        return this;
    }

    public FlowContentBuilder setJsar(byte[] jsar) {
        this.jsar = jsar;
        return this;
    }

    public FlowContentBuilder setTimeOfLastModification(Date timeOfLastModification) {
        this.timeOfLastModification = timeOfLastModification;
        return this;
    }

    public FlowContentBuilder setComponents(List<FlowComponent> components) {
        this.components = new ArrayList<>(components);
        return this;
    }

    public FlowContentBuilder setTimeOfFlowComponentUpdate(Date timeOfFlowComponentUpdate) {
        this.timeOfFlowComponentUpdate = timeOfFlowComponentUpdate;
        return this;
    }

    public FlowContent build() {
        return new FlowContent(name, description, entrypointScript, entrypointFunction, jsar, timeOfLastModification, components, timeOfFlowComponentUpdate);
    }
}
