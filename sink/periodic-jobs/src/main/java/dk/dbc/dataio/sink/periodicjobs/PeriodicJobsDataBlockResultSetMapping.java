package dk.dbc.dataio.sink.periodicjobs;

import jakarta.persistence.PersistenceException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

public class PeriodicJobsDataBlockResultSetMapping implements Function<ResultSet, PeriodicJobsDataBlock> {
    @Override
    public PeriodicJobsDataBlock apply(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                final PeriodicJobsDataBlock datablock = new PeriodicJobsDataBlock();
                datablock.setKey(new PeriodicJobsDataBlock.Key(
                        resultSet.getInt("JOBID"),
                        resultSet.getInt("RECORDNUMBER"),
                        resultSet.getInt("RECORDPART")));
                datablock.setSortkey(resultSet.getString("SORTKEY"));
                datablock.setBytes(resultSet.getBytes("BYTES"));
                datablock.setGroupHeader(resultSet.getBytes("GROUPHEADER"));
                return datablock;
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        }
        return null;
    }
}
