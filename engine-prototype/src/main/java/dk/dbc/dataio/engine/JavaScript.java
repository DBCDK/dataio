package dk.dbc.dataio.engine;

public class JavaScript {
    private final String javascript;
    private final String moduleName;

    public JavaScript(String javascript, String moduleName) {
        this.javascript = javascript;
        this.moduleName = moduleName;
    }

    public String getJavascript() {
        return javascript;
    }

    public String getModuleName() {
        return moduleName;
    }
}
