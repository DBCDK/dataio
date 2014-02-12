package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.JavaScript;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FlowComponentContentBuilder {
    private String name = "name";
    private String svnProject = "svnproject";
    private long svnRevision = 1L;
    private List<JavaScript> javascripts = new ArrayList<>(Arrays.asList(
            new JavaScriptBuilder().build()));
    private String invocationMethod = "invocationMethod";

    public FlowComponentContentBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public FlowComponentContentBuilder setSvnProject(String project) {
        this.svnProject = project;
        return this;
    }

    public FlowComponentContentBuilder setSvnRevision(long revision) {
        this.svnRevision = revision;
        return this;
    }

    public FlowComponentContentBuilder setJavascripts(List<JavaScript> javascripts) {
        this.javascripts = new ArrayList<>(javascripts);
        return this;
    }

    public FlowComponentContentBuilder setInvocationMethod(String invocationMethod) {
        this.invocationMethod = invocationMethod;
        return this;
    }

    public FlowComponentContent build() {
        return new FlowComponentContent(name, svnProject, svnRevision, javascripts, invocationMethod);
    }
}
