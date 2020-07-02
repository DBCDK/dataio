/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

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
            model = new JobModel();
        }
        View.JobStatus jobStatus = View.JobStatus.DONE_WITHOUT_ERROR; // Default value

        // Check if the job has failed before partitioning
        if(model.getDiagnosticModels().size() != 0) {
            jobStatus = View.JobStatus.DONE_WITH_ERROR;
        }
        else {
            // Check if the job is completely done
            if (model.getJobCompletionTime() == null || model.getJobCompletionTime().isEmpty()) {
                jobStatus = View.JobStatus.NOT_DONE;
            }
            // If the job is done: Check if any errors has occurred.
            else if (model.getStateModel().getFailedCounter() != 0 || model.isDiagnosticFatal()) {
                jobStatus = View.JobStatus.DONE_WITH_ERROR;
            }
            else if(model.getNumberOfItems() != 0 && model.getNumberOfChunks() == 0) {
                jobStatus = View.JobStatus.PREVIEW;
            }
        }
        return jobStatus;
    }

}
