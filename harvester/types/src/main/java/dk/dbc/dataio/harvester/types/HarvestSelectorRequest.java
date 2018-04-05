/*
 * DataIO - Data IO
 *
 * Copyright (C) 2017 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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
