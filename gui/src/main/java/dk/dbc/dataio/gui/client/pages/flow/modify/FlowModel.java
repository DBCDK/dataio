package dk.dbc.dataio.gui.client.pages.flow.modify;

import dk.dbc.dataio.gui.client.model.GenericBackendModel;

import java.util.HashMap;
import java.util.Map;

public class FlowModel extends GenericBackendModel {

    private String flowName;
    private String description;
    private Map<String, String> flowComponents;

    public FlowModel(long id, long version, String name, String description, Map<String, String> flowComponents) {
        super(id, version);
        this.flowName = name;
        this.description = description;
        this.flowComponents = flowComponents;
    }

    public FlowModel() {
        super(0L, 0L);
        this.flowName = "";
        this.description = "";
        this.flowComponents = new HashMap<String, String>();
    }

    /**
     * @return flowName The name of the flow
     */
    public String getFlowName() {
        return flowName;
    }

    /**
     * Set flow name
     * @param flowName The name of the flow
     */
    public void setFlowName(String flowName) {
        this.flowName = flowName;
    }

    /**
     * @return description The flow description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set flow description
     * @param description The flow description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return The list of flowcomponents in the flow
     */
    public Map<String, String> getFlowComponents() {
        return flowComponents;
    }

    /**
     * Set the list of flowcomponents in the flow
     * @param flowComponents The list of flowcomponents
     */
    public void setFlowComponents(Map<String, String> flowComponents) {
        this.flowComponents = flowComponents;
    }

    /**
     * Checks for empty String values
     */
    public boolean isInputFieldsEmpty() {
        return flowName.isEmpty() || description.isEmpty() || flowComponents.isEmpty();
    }

}
