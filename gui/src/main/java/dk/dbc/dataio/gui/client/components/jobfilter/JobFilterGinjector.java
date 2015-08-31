package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;

/**
 * This is the list of Dependency Injections for the JobFilter
 * They are directly callable methods, returning instance objects of the given type
 * Underlying Dependency Injections will automatically be injected
 */
@GinModules(JobFilterClientModule.class)
public interface JobFilterGinjector extends Ginjector {
    SinkJobFilter getSinkJobFilter();
    SubmitterJobFilter getSubmitterJobFilter();
    SuppressSubmitterJobFilter getSuppressSubmitterJobFilter();
}
