package dk.dbc.dataio.cli.options;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;

import java.util.HashMap;
import java.util.Map;

public class Options {
    @Parameter(names = {"-h", "--help"}, description = "Show this help", help = true)
    public boolean help;

    @Parameter(names = {"-g", "--gui-url"}, description = "URL of dataIO gui")
    public String guiUrl = "http://dataio.dbc.dk";

    @DynamicParameter(names = "--override-endpoint")
    public Map<String, String> overriddenEndpoints = new HashMap<>();
}
