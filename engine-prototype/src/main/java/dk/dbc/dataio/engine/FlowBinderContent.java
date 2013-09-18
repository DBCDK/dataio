package dk.dbc.dataio.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * FlowBinderContent DTO class.
 *
 * In all essence objects of this class are immutable, but due to GWT serialization
 * issues we cannot have final fields and need a default no-arg constructor.
 */
public class FlowBinderContent implements Serializable {
    private static final long serialVersionUID = 1106844598199379043L;

    private /* final */ String name;
    private /* final */ String description;
    private /* final */ String packaging;   // rammeformat
    private /* final */ String format;      // indholdsformat
    private /* final */ String charset;
    private /* final */ String destination;
    private /* final */ String recordSplitter;
    private /* final */ Long flowId;
    private /* final */ List<Long> submitterIds;

    private FlowBinderContent() { }

    public FlowBinderContent(String name, String description, String packaging, String format, String charset, String destination, String recordSplitter, Long flowId, List<Long> submitterIds) {
        this.name = name;
        this.description = description;
        this.packaging = packaging;
        this.format = format;
        this.charset = charset;
        this.destination = destination;
        this.recordSplitter = recordSplitter;
        this.flowId = flowId;
        this.submitterIds = new ArrayList<Long>(submitterIds);
    }

    public String getCharset() {
        return charset;
    }

    public String getDescription() {
        return description;
    }

    public String getDestination() {
        return destination;
    }

    public Long getFlowId() {
        return flowId;
    }

    public String getFormat() {
        return format;
    }

    public String getName() {
        return name;
    }

    public String getPackaging() {
        return packaging;
    }

    public String getRecordSplitter() {
        return recordSplitter;
    }

    public List<Long> getSubmitterIds() {
        return new ArrayList<Long>(submitterIds);
    }
}
