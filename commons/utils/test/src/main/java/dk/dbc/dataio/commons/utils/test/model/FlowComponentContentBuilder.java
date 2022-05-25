package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.JavaScript;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlowComponentContentBuilder {
    private String name = "name";
    private String description = "description";
    private String svnProjectForInvocationJavascript = "svnprojectforinvocationjavascript";
    private long svnRevision = 1L;
    private String invocationJavascriptName = "invocationJavascriptName";
    private List<JavaScript> javascripts = new ArrayList<>(Collections.singletonList(
            new JavaScriptBuilder().build()));
    private String invocationMethod = "invocationMethod";
    private String requireCache = null;

    public FlowComponentContentBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public FlowComponentContentBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public FlowComponentContentBuilder setSvnProjectForInvocationJavascript(String project) {
        this.svnProjectForInvocationJavascript = project;
        return this;
    }

    public FlowComponentContentBuilder setSvnRevision(long revision) {
        this.svnRevision = revision;
        return this;
    }

    public FlowComponentContentBuilder setInvocationJavascriptName(String invocationJavascriptName) {
        this.invocationJavascriptName = invocationJavascriptName;
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

    public FlowComponentContentBuilder setRequireCache(String requireCache) {
        this.requireCache = requireCache;
        return this;
    }

    public FlowComponentContent build() {
        return new FlowComponentContent(name, svnProjectForInvocationJavascript, svnRevision, invocationJavascriptName, javascripts, invocationMethod, description, requireCache);
    }
}
