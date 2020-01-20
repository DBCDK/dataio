/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.periodicjobs;

import javax.persistence.PersistenceException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

public class PeriodicJobsDataBlockResultSetMapping implements Function<ResultSet, PeriodicJobsDataBlock> {
    @Override
    public PeriodicJobsDataBlock apply(java.sql.ResultSet resultSet) {
        if (resultSet != null) {
            try {
                final PeriodicJobsDataBlock datablock = new PeriodicJobsDataBlock();
                datablock.setKey(new PeriodicJobsDataBlock.Key(
                        resultSet.getInt("JOBID"),
                        resultSet.getInt("RECORDNUMBER")));
                datablock.setSortkey(resultSet.getString("SORTKEY"));
                datablock.setBytes(resultSet.getBytes("BYTES"));
                return datablock;
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        }
        return null;
    }
}
