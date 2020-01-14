/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.periodicjobs;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = "datablock")
@NamedQueries({
    @NamedQuery(
            name = PeriodicJobsDataBlock.GET_DATA_BLOCKS_QUERY_NAME,
            query = PeriodicJobsDataBlock.GET_DATA_BLOCKS_QUERY),
    @NamedQuery(
            name = PeriodicJobsDataBlock.DELETE_DATA_BLOCKS_QUERY_NAME,
            query = PeriodicJobsDataBlock.DELETE_DATA_BLOCKS_QUERY)
})
public class PeriodicJobsDataBlock {
    public static final String GET_DATA_BLOCKS_QUERY =
            "SELECT datablock FROM PeriodicJobsDataBlock datablock" +
            " WHERE datablock.jobId = ?1" +
            " ORDER BY datablock.sortkey ASC";
    public static final String GET_DATA_BLOCKS_QUERY_NAME =
            "PeriodicJobsDataBlock.get";
    public static final String DELETE_DATA_BLOCKS_QUERY =
            "DELETE FROM PeriodicJobsDataBlock datablock" +
            " WHERE datablock.jobId = :jobId";
    public static final String DELETE_DATA_BLOCKS_QUERY_NAME =
            "PeriodicJobsDataBlock.delete";

    @Id
    private Integer id;

    private String sortkey;

    private Integer jobId;

    @Lob
	private byte[] bytes;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getJobId() {
        return jobId;
    }

    public void setJobId(Integer jobId) {
        this.jobId = jobId;
    }

    public String getSortkey() {
        return sortkey;
    }

    public void setSortkey(String sortkey) {
        this.sortkey = sortkey;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }
}
