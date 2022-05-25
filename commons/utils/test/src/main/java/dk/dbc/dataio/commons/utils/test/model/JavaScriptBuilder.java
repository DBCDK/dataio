package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.JavaScript;

public class JavaScriptBuilder {
    private String javascript = "javascript";
    private String moduleName = "moduleName";

    public JavaScriptBuilder setJavascript(String javascript) {
        this.javascript = javascript;
        return this;
    }

    public JavaScriptBuilder setModuleName(String moduleName) {
        this.moduleName = moduleName;
        return this;
    }

    public JavaScript build() {
        return new JavaScript(javascript, moduleName);
    }
}
