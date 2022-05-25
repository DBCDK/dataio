package dk.dbc.dataio.gui.client.modelBuilders;

import dk.dbc.dataio.gui.client.model.FlowComponentModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FlowComponentModelBuilder {

    private long id = 1L;
    private long version = 1L;
    private String name = "name";
    private String svnProject = "svnProject";
    private String svnRevision = "42";
    private String svnNext = "45";
    private String invocationJavascript = "invocationJavascript";
    private String invocationMethod = "invocationMethod";
    private List<String> javascriptModules = new ArrayList<>(Arrays.asList("javascript1", "javascript2"));
    private List<String> nextJavascriptModules = new ArrayList<>(Arrays.asList("javascript3", "javascript4"));
    private String description = "description";

    public FlowComponentModelBuilder setId(long id) {
        this.id = id;
        return this;
    }

    public FlowComponentModelBuilder setVersion(long version) {
        this.version = version;
        return this;
    }

    public FlowComponentModelBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public FlowComponentModelBuilder setSvnProject(String svnProject) {
        this.svnProject = svnProject;
        return this;
    }

    public FlowComponentModelBuilder setSvnRevision(String svnRevision) {
        this.svnRevision = svnRevision;
        return this;
    }

    public FlowComponentModelBuilder setSvnNext(String svnNext) {
        this.svnNext = svnNext;
        return this;
    }

    public FlowComponentModelBuilder setInvocationJavascript(String invocationJavascript) {
        this.invocationJavascript = invocationJavascript;
        return this;
    }


    public FlowComponentModelBuilder setInvocationMethod(String invocationMethod) {
        this.invocationMethod = invocationMethod;
        return this;
    }

    public FlowComponentModelBuilder setJavascriptModules(List<String> javascriptModules) {
        this.javascriptModules = javascriptModules;
        return this;
    }

    public FlowComponentModelBuilder setNextJavascriptModules(List<String> nextJavascriptModules) {
        this.nextJavascriptModules = nextJavascriptModules;
        return this;
    }

    public FlowComponentModelBuilder setDescription(String description) {
        this.description = description;
        return this;
    }


    public FlowComponentModel build() {
        return new FlowComponentModel(id, version, name, svnProject, svnRevision, svnNext, invocationJavascript, invocationMethod, javascriptModules, nextJavascriptModules, description);
    }

}
