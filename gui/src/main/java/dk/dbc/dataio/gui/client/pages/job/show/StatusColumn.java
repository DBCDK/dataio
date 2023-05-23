package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.cellview.client.Column;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.resources.Resources;

/**
 * This class is a specialization of the Column class
 * It contains knowledge about how to map JobModel to the actual Icon to be displayed.
 */
class StatusColumn extends Column<JobModel, ImageResource> {

    // Attributes
    private final Resources resources;

    /**
     * Default constructor
     *
     * @param resources The resource containing the images to be used in the display of the icons
     * @param cell      The Image to put into the status column cell
     */
    public StatusColumn(Resources resources, Cell<ImageResource> cell) {
        super(cell);
        this.resources = resources;
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
            case PREVIEW:
                return resources.yellow();
            case ABORTED:
                return resources.deleteDownButton();
            default:
                return resources.green();
        }
    }

    /**
     * This method maps the logical job status to a JobStatusEnum
     *
     * @param model The JobInfo for the job to test
     * @return JobStatusEnum: NOT_DONE, DONE_WITHOUT_ERROR or DONE_WITH_ERROR
     */
    private View.JobStatus getJobStatus(JobModel model) {
        if (model == null) {
            return View.JobStatus.NOT_DONE;
        }

        if (model.getStateModel().isAborted()) {
            return View.JobStatus.ABORTED;
        }
        // Check if the job has failed before partitioning
        if (!model.getDiagnosticModels().isEmpty()){
            return View.JobStatus.DONE_WITH_ERROR;
        }
        // Check if the job is completely done
        if (model.getJobCompletionTime() == null || model.getJobCompletionTime().isEmpty()) {
            return View.JobStatus.NOT_DONE;
        }
        // If the job is done: Check if any errors has occurred.
        if (model.getStateModel().getFailedCounter() != 0 || model.isDiagnosticFatal()) {
            return View.JobStatus.DONE_WITH_ERROR;
        }
        if (model.getNumberOfItems() != 0 && model.getNumberOfChunks() == 0) {
            return View.JobStatus.PREVIEW;
        }
        return View.JobStatus.DONE_WITHOUT_ERROR;
    }

}
