/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.util;

import com.google.gwt.event.shared.SimpleEventBus;
import com.google.web.bindery.event.shared.EventBus;
import dk.dbc.dataio.gui.views.FlowEditView;
import dk.dbc.dataio.gui.views.FlowEditViewImpl;

/**
 *
 * @author slf
 */
public class ClientFactoryImpl implements ClientFactory {
	private final EventBus eventBus = new SimpleEventBus();

    // Models
	
	// Views
	private FlowEditView flowEditView = new FlowEditViewImpl();

    // Presenters

    
    @Override
	public EventBus getEventBus() {
		return eventBus;
	}

    @Override
    public FlowEditView getFlowEditView() {
        return flowEditView;
    }


}
