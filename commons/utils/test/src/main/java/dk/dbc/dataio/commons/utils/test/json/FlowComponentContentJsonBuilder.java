package dk.dbc.dataio.commons.utils.test.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FlowComponentContentJsonBuilder extends JsonBuilder {
    private String name = "name";
    private String svnProject = "svnproject";
    private long svnRevision = 1L;
    private String javaScriptName = "javascriptname";
    private List<String> javascripts = new ArrayList<>(Arrays.asList(
            new JavaScriptJsonBuilder().build()));
    private String invocationMethod = "invocationMethod";

    public FlowComponentContentJsonBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public FlowComponentContentJsonBuilder setSvnProject(String project) {
        this.svnProject = project;
        return this;
    }

    public FlowComponentContentJsonBuilder setSvnRevision(long revision) {
        this.svnRevision = revision;
        return this;
    }

    public FlowComponentContentJsonBuilder setJavaScriptName(String javaScriptName) {
        this.javaScriptName = javaScriptName;
        return this;
    }

    public FlowComponentContentJsonBuilder setJavascripts(List<String> javascripts) {
        this.javascripts = new ArrayList<>(javascripts);
        return this;
    }

    public FlowComponentContentJsonBuilder setInvocationMethod(String invocationMethod) {
        this.invocationMethod = invocationMethod;
        return this;
    }

   public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);
        stringBuilder.append(asTextMember("name", name)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("svnProject", svnProject)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asLongMember("svnRevision", svnRevision)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("javaScriptName", javaScriptName)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("invocationMethod", invocationMethod)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asObjectArray("javascripts", javascripts));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}
