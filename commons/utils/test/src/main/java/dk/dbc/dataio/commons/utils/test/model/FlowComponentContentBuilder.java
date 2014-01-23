package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.JavaScript;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FlowComponentContentBuilder {
    private String name = "name";
    private String invocationMethod = "invocationMethod";
    private List<JavaScript> javascripts = new ArrayList<>(Arrays.asList(
            new JavaScriptBuilder().build()));

    public FlowComponentContentBuilder setInvocationMethod(String invocationMethod) {
        this.invocationMethod = invocationMethod;
        return this;
    }

    public FlowComponentContentBuilder setJavascripts(List<JavaScript> javascripts) {
        this.javascripts = new ArrayList<>(javascripts);
        return this;
    }

    public FlowComponentContentBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public FlowComponentContent build() {
        return new FlowComponentContent(name, javascripts, invocationMethod);
    }
}
