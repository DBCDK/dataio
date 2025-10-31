package dk.dbc.dataio.harvester.utils.rawrepo;

import dk.dbc.pgqueue.common.QueueStorageAbstraction;
import dk.dbc.rawrepo.dto.RecordIdDTO;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RRDM3ItemStorage implements QueueStorageAbstraction<RecordIdDTO> {
    private static final String[] columnList = {"bibliographicrecordid", "agencyid"};

    @Override
    public String[] columnList() {
        return columnList;
    }

    @Override
    public RecordIdDTO createJob(ResultSet resultSet, int startColumn) throws SQLException {
        return new RecordIdDTO(resultSet.getString(startColumn++),  resultSet.getInt(startColumn));
    }

    @Override
    public void saveJob(RecordIdDTO job, PreparedStatement stmt, int startColumn) throws SQLException {
        stmt.setString(startColumn++, job.getBibliographicRecordId());
        stmt.setInt(startColumn, job.getAgencyId());
    }
}
