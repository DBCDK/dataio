package dk.dbc.dataio.engine;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.nio.file.Path;

@SuppressWarnings("unused")
abstract class JobJsonMixIn {
    @JsonCreator
    public JobJsonMixIn(@JsonProperty("id") long id, @JsonProperty("originalDataPath") Path originalDataPath, @JsonProperty("flow") Flow flow) { }
}
