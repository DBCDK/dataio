package dk.dbc.dataio.commons.utils.test.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FlowComponentContentJsonBuilder extends JsonBuilder {
    private String name = "name";
    private String invocationMethod = "invocationMethod";
    private List<String> javascripts = new ArrayList<>(Arrays.asList(
            new JavaScriptJsonBuilder().build()));

    public FlowComponentContentJsonBuilder setInvocationMethod(String invocationMethod) {
        this.invocationMethod = invocationMethod;
        return this;
    }

    public FlowComponentContentJsonBuilder setJavascripts(List<String> javascripts) {
        this.javascripts = new ArrayList<>(javascripts);
        return this;
    }

    public FlowComponentContentJsonBuilder setName(String name) {
        this.name = name;
        return this;
    }

   public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);
       stringBuilder.append(asTextMember("name", name)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("invocationMethod", invocationMethod)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asObjectArray("javascripts", javascripts));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}
