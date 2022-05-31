package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import dk.dbc.dataio.gui.client.components.MultiProgressBar;
import dk.dbc.dataio.gui.client.model.JobModel;


/**
 * This class is a specialization of the Column class
 * It contains knowledge about how to map JobModel to the actual Progress Bar to be displayed.
 */
public class ProgressColumn extends Column<JobModel, MultiProgressBar> {

    /**
     * This class is a specialization of the Cell class
     * It contains knowledge about how to render a Progress Bar in a cell
     */
    static class ProgressCell extends AbstractCell<MultiProgressBar> implements Cell<MultiProgressBar> {
        @Override
        public void render(Context context, MultiProgressBar progressBar, SafeHtmlBuilder sb) {
            if (progressBar != null) {
                sb.appendHtmlConstant(progressBar.getElement().getInnerHTML());
            }
        }
    }


    /**
     * Default constructor
     */
    public ProgressColumn() {
        super(new ProgressCell());
    }

    /**
     * This method instantiates a new MultiProgressBar widget, and takes its values from the supplied JobModel parameter
     *
     * @param model The job model, that contains the values to be displayed in the Progress Bar
     * @return The newly instantiated MultiProgressBar widget
     */
    @Override
    public MultiProgressBar getValue(JobModel model) {
        if (model == null) {
            model = new JobModel();
        }
        final String DELIMITER = "/";
        String delivered = String.valueOf(model.getStateModel().getDeliveredCounter());
        String processed = String.valueOf(model.getStateModel().getProcessedCounter());
        String processedAndNotDone = String.valueOf(model.getStateModel().getProcessedCounter() < model.getStateModel().getDeliveredCounter() ? 0 : model.getStateModel().getProcessedCounter() - model.getStateModel().getDeliveredCounter());
        String remaining = String.valueOf(model.getNumberOfItems() < model.getStateModel().getProcessedCounter() ? 0 : model.getNumberOfItems() - model.getStateModel().getProcessedCounter());
        String total = String.valueOf(model.getNumberOfItems());
        String caption = delivered + DELIMITER + processedAndNotDone + DELIMITER + remaining;
        return new MultiProgressBar(caption, delivered, processed, total);
    }

}
