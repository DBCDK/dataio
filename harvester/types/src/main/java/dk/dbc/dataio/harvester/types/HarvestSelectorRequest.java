package dk.dbc.dataio.harvester.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.invariant.InvariantUtil;

public class HarvestSelectorRequest extends HarvestRequest<HarvestSelectorRequest> {
    @JsonProperty("selector")
    private final String selectorExpression;  //NOPMD - field is used when marshalling to JSON
    @JsonIgnore
    private final HarvestTaskSelector selector;

    @JsonCreator
    public HarvestSelectorRequest(@JsonProperty("selector") String selectorExpression)
            throws NullPointerException, IllegalArgumentException {
        this.selectorExpression = InvariantUtil.checkNotNullNotEmptyOrThrow(selectorExpression, "selectorExpression");
        this.selector = HarvestTaskSelector.of(selectorExpression);
    }

    public HarvestSelectorRequest(HarvestTaskSelector selector) throws NullPointerException {
        this.selector = InvariantUtil.checkNotNullOrThrow(selector, "selector");
        this.selectorExpression = selector.toString();
    }

    @JsonIgnore
    public HarvestTaskSelector getSelector() {
        return selector;
    }
}
