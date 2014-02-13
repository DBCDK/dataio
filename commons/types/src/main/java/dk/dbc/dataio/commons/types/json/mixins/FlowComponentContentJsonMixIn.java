package dk.dbc.dataio.commons.types.json.mixins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.JavaScript;
import java.util.List;

/**
 * This class is a companion to the FlowComponentContent DTO class.
 *
 * Think of this as a way to keep the DTO class "jackson-free" by mixing in
 * annotations to the DTO class during runtime.
 *
 * Method implementations of a MixIn class are ignored.
 */
@SuppressWarnings("unused")
public class FlowComponentContentJsonMixIn {

    /**
     * Makes jackson runtime aware of non-default FlowComponentContent
     * constructor.
     */
    @JsonCreator
    public FlowComponentContentJsonMixIn(@JsonProperty("name") String name,
                                         @JsonProperty("svnProjectForInvocationJavascript") String svnProjectForInvocationJavascript,
                                         @JsonProperty("svnRevision") long svnRevision,
                                         @JsonProperty("javaScriptName") String javaScriptName,
                                         @JsonProperty("javascripts") List<JavaScript> javascripts,
                                         @JsonProperty("invocationMethod") String invocationMethod) {
    }
}
