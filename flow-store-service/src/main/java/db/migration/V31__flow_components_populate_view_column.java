/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package db.migration;

import dk.dbc.dataio.flowstore.entity.FlowComponent;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.ResultSet;
import java.sql.Statement;

public class V31__flow_components_populate_view_column extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        try (Statement select = context.getConnection().createStatement()) {
            try (ResultSet rows = select.executeQuery("SELECT id, version, content FROM flow_components")) {
                while (rows.next()) {
                    final FlowComponent flowComponent = new FlowComponent();
                    flowComponent.setId(rows.getLong(1));
                    flowComponent.setVersion(rows.getLong(2));
                    flowComponent.setContent(rows.getString(3));
                    try (Statement update = context.getConnection().createStatement()) {
                        update.execute("UPDATE flow_components SET view='" + flowComponent.generateView().replaceAll("'", "''") + "' WHERE id=" + flowComponent.getId());
                    }
                }
            }
        }
    }
}
