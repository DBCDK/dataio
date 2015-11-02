package dk.dbc.dataio.jobstore.test.types;

import dk.dbc.dataio.jobstore.types.IncompleteTransfileNotificationContext;

/**
 * Created by ThomasBerg on 02/11/15.
 */
public class IncompleteTransfileNotificationContextBuilder {

    private final String transfileName;       // required
    private final String transfileContent;    // required

    public IncompleteTransfileNotificationContextBuilder(String transfileName, String transfileContent) {
        this.transfileName = transfileName;
        this.transfileContent = transfileContent;
    }

    public IncompleteTransfileNotificationContext build() {
        return new IncompleteTransfileNotificationContext(this.transfileName, this.transfileContent);
    }
}
