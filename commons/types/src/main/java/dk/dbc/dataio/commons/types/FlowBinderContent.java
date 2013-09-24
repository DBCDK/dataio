package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

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
    private /* final */ String packaging;
    private /* final */ String format;
    private /* final */ String charset;
    private /* final */ String destination;
    private /* final */ String recordSplitter;
    private /* final */ long flowId;
    private /* final */ List<Long> submitterIds;

    private FlowBinderContent() { }

    /**
     * Class constructor
     *
     * @param name flowbinder name
     * @param description flowbinder description
     * @param packaging flowbinder packaging (rammeformat)
     * @param format flowbinder format (indholdsformat)
     * @param charset flowbinder character set
     * @param destination flow binder destination
     * @param recordSplitter flow binder record splitter
     * @param flowId id of flow attached to this flowbinder
     * @param submitterIds ids of submitters attached to this flowbinder
     *
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty valued String or List argument
     */
    public FlowBinderContent(String name, String description, String packaging, String format, String charset, String destination, String recordSplitter, long flowId, List<Long> submitterIds) {
        this.name = InvariantUtil.checkNotNullNotEmptyOrThrow(name, "name");
        this.description = InvariantUtil.checkNotNullNotEmptyOrThrow(description, "description");
        this.packaging = InvariantUtil.checkNotNullNotEmptyOrThrow(packaging, "packaging");
        this.format = InvariantUtil.checkNotNullNotEmptyOrThrow(format, "format");
        this.charset = InvariantUtil.checkNotNullNotEmptyOrThrow(charset, "charset");
        this.destination = InvariantUtil.checkNotNullNotEmptyOrThrow(destination, "destination");
        this.recordSplitter = InvariantUtil.checkNotNullNotEmptyOrThrow(recordSplitter, "recordSplitter");
        this.flowId = flowId;
        this.submitterIds = new ArrayList<Long>(InvariantUtil.checkNotNullOrThrow(submitterIds, "submitterIds"));
        if (this.submitterIds.size() == 0) {
            throw new IllegalArgumentException("submitterIds can not be empty");
        }
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

    public long getFlowId() {
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
