package dk.dbc.dataio.jobstore.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

/**
 * Created by ThomasBerg on 26/10/15.
 */
public class IncompleteTransfileNotificationContext {

    private String transfileName;
    private String transfileContent;

    public IncompleteTransfileNotificationContext(String transfileName, String transfileContent) {
        InvariantUtil.checkNotNullOrThrow(transfileName, "transfileName");
        InvariantUtil.checkNotNullOrThrow(transfileContent, "transfileContent");

        this.transfileName = transfileName;
        this.transfileContent = transfileContent;
    }
    public String getTransfileName() {
        return transfileName;
    }

    public String getTransfileContent() {
        return transfileContent;
    }
}