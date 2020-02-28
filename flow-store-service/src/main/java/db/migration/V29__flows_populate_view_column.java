/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package db.migration;

import dk.dbc.dataio.flowstore.entity.Flow;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.ResultSet;
import java.sql.Statement;

public class V29__flows_populate_view_column extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        try (Statement select = context.getConnection().createStatement()) {
            try (ResultSet rows = select.executeQuery("SELECT id, version, content FROM flows")) {
                while (rows.next()) {
                    final Flow flow = new Flow();
                    flow.setId(rows.getLong(1));
                    flow.setVersion(rows.getLong(2));
                    flow.setContent(rows.getString(3));
                    try (Statement update = context.getConnection().createStatement()) {
                        update.execute("UPDATE flows SET view='" + flow.generateView().replaceAll("'", "''") + "' WHERE id=" + flow.getId());
                    }
                }
            }
        }
    }
}
