package dk.dbc.dataio.gui.client.model;

import dk.dbc.dataio.gui.client.util.Format;

import java.util.ArrayList;
import java.util.List;

public class FlowModel extends GenericBackendModel {

    private String flowName;
    private String description;
    private List<FlowComponentModel> flowComponents;

    public FlowModel(long id, long version, String name, String description, List<FlowComponentModel> flowComponents) {
        super(id, version);
        this.flowName = name;
        this.description = description;
        this.flowComponents = flowComponents;
    }

    public FlowModel() {
        super(0L, 0L);
        this.flowName = "";
        this.description = "";
        this.flowComponents = new ArrayList<FlowComponentModel>();
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
    public List<FlowComponentModel> getFlowComponents() {
        return flowComponents;
    }

    /**
     * Set the list of flowcomponents in the flow
     * @param flowComponents The list of flowcomponents
     */
    public void setFlowComponents(List<FlowComponentModel> flowComponents) {
        this.flowComponents = flowComponents;
    }

    /**
     * Checks for empty String values
     */
    public boolean isInputFieldsEmpty() {
        return flowName == null
                || flowName.isEmpty()
                || description == null
                || description.isEmpty()
                || flowComponents == null
                || flowComponents.isEmpty();
    }

    /**
     * Checks if the flow name contains illegal characters.
     * A-Å, 0-9, - (minus), + (plus), _ (underscore) and space is valid
     * @return a list containing illegal characters found. Empty list if none found.
     */
    public List<String> getDataioPatternMatches() {
        return Format.getDataioPatternMatches(flowName);
    }

}
