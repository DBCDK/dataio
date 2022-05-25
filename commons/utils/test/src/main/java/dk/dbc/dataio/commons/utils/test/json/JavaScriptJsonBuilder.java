package dk.dbc.dataio.commons.utils.test.json;

public class JavaScriptJsonBuilder extends JsonBuilder {
    private String javascript = "javascript";
    private String moduleName = "moduleName";

    public JavaScriptJsonBuilder setJavascript(String javascript) {
        this.javascript = javascript;
        return this;
    }

    public JavaScriptJsonBuilder setModuleName(String moduleName) {
        this.moduleName = moduleName;
        return this;
    }

    public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);
        stringBuilder.append(asTextMember("javascript", javascript));
        stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("moduleName", moduleName));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}
