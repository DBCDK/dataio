/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import java.util.List;

/**
 *
 * @author damkjaer
 */
class ViewPage extends VerticalPanel {

    private final DataContentObject dataContentObject;

    public ViewPage(DataContentObject dataContentObject) {
        this.dataContentObject = dataContentObject;
    }

    public void onUpdate() {
        List<String> contentList = dataContentObject.getAll();
        clear();
        for (int i = 0; i < contentList.size(); i++) {
            HorizontalPanel contentPanel = new HorizontalPanel();
            Label indexLabel = new Label(Integer.toString(i));
            Label contentLabel = new Label(contentList.get(i));
            contentPanel.add(indexLabel);
            contentPanel.add(contentLabel);
            add(contentPanel);
        }
    }
}
