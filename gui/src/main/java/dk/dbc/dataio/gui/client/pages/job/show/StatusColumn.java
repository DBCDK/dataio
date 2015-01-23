package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.cellview.client.Column;
import com.google.web.bindery.event.shared.EventBus;
import dk.dbc.dataio.commons.types.JobErrorCode;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.panels.statuspopup.StatusPopup;
import dk.dbc.dataio.gui.client.resources.Resources;

/**
 * This class is a specialization of the Column class
 * It contains knowledge about how to map JobModel to the actual Icon to be displayed,
 * and what to do, when clicking on the Icon (display the Status Popup Panel)
 */
class StatusColumn extends Column<JobModel, ImageResource> {
    // Attributes
    private final EventBus eventBus;
    private final Resources resources;


    /**
     * Default constructor
     *
     * @param eventBus  The eventBus to be used in the communication between this view and its popup panel
     * @param resources The resource containing the images to be used in the display of the icons
     * @param cell      The Image to put into the status column cell
     */
    public StatusColumn(EventBus eventBus, Resources resources, Cell<ImageResource> cell) {
        super(cell);
        this.eventBus = eventBus;
        this.resources = resources;
    }

    /**
     * Event handler for handling browser events
     *
     * @param context The Cell.Context in which the event originates
     * @param parent  The element in which the event originates
     * @param model   The JobModel for the actual event
     * @param event   The event
     */
    @Override
    public void onBrowserEvent(Cell.Context context, Element parent, JobModel model, NativeEvent event) {
        super.onBrowserEvent(context, parent, model, event);
        if ("click".equals(event.getType())) {
            new StatusPopup(eventBus, parent, model);
        }
    }

    /**
     * This method gets an image for a given model
     *
     * @param model The model
     * @return The image for the given model
     */
    @Override
    public ImageResource getValue(JobModel model) {
        switch (getJobStatus(model)) {
            case NOT_DONE:
                return resources.gray();
            case DONE_WITH_ERROR:
                return resources.red();
            default:
                return resources.green();
        }
    }

    /**
     * This method maps the logical jobstatus to a JobStatusEnum
     *
     * @param model The JobInfo for the job to test
     * @return JobStatusEnum: NOT_DONE, DONE_WITHOUT_ERROR or DONE_WITH_ERROR
     */
    private View.JobStatus getJobStatus(JobModel model) {
        View.JobStatus jobStatus = View.JobStatus.DONE_WITHOUT_ERROR; // Default value
        if (model.getJobErrorCode() != JobErrorCode.NO_ERROR) {
            // The entire job has failed
            jobStatus = View.JobStatus.DONE_WITH_ERROR;
        } else {
            //Check if the job is completely done
            if (!model.getJobDone()) {
                jobStatus = View.JobStatus.NOT_DONE;
            } else if (model.getChunkifyingFailureCounter() > 0L
                    || model.getProcessingFailureCounter() > 0L
                    || model.getDeliveringFailureCounter() > 0L) {
                // Errors found
                jobStatus = View.JobStatus.DONE_WITH_ERROR;
            }
        }
        return jobStatus;
    }

}
