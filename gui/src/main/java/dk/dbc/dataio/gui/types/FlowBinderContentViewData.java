
package dk.dbc.dataio.gui.types;

import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * FlowBinderContentViewData Class.
 *
 * This class is used in the communication between the Activities and the Views
 *
 */
public class FlowBinderContentViewData extends FlowBinderContent {

    private String flowName;
    private String sinkName;
    private List<SubmitterContent> submitterContents;

    public FlowBinderContentViewData(String name, String description, String packaging, String format, String charset, String destination, String recordSplitter,
                                    Long flowId, String flowName,
                                    List<Long> submitterIds, List<SubmitterContent> submitterContents,
                                    Long sinkId, String sinkName) {
        super(name, description, packaging, format, charset, destination, recordSplitter, flowId, submitterIds, sinkId);
        this.flowName = InvariantUtil.checkNotNullNotEmptyOrThrow(flowName, "flowName");
        this.sinkName = InvariantUtil.checkNotNullNotEmptyOrThrow(sinkName, "sinkName");
        this.submitterContents = new ArrayList<SubmitterContent>(InvariantUtil.checkNotNullOrThrow(submitterContents, "submitterContents"));
        if (submitterContents.isEmpty()) {
            throw new IllegalArgumentException("submitterContents can not be empty");
        }
    }

    public String getFlowName() {
        return flowName;
    }

    public String getSinkName() {
        return sinkName;
    }

    public List<SubmitterContent> getSubmitters() {
        return new ArrayList<SubmitterContent>(submitterContents);
    }

}
