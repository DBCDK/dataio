/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.holdingsitems;

import com.fasterxml.jackson.databind.type.CollectionType;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.solrdocstore.connector.SolrDocStoreConnector;
import dk.dbc.solrdocstore.connector.model.HoldingsItems;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

/**
 * This class is responsible for unmarshalling byte[] representation of JSON array of holdings items
 * into List of {@link HoldingsItems}.
 *
 * This class also tests the existence of already created holdings items against the input in order
 * to delete any deprecated entries in accordance with the "248 datafield" model.
 */
@ApplicationScoped
public class HoldingsItemsUnmarshaller {
    private final JSONBContext jsonbContext = new JSONBContext();
    private final CollectionType holdingsItemsListType = jsonbContext.getTypeFactory()
            .constructCollectionType(List.class, HoldingsItems.class);

    @Inject SolrDocStoreConnector solrDocStoreConnector;

    public List<HoldingsItems> unmarshall(byte[] bytes) throws JSONBException {
        final List<HoldingsItems> holdingsItemsList = jsonbContext.unmarshall(
                StringUtil.asString(bytes), holdingsItemsListType);

        // TODO: 10/05/2021 deletions based on 248

        return holdingsItemsList;
    }
}
