package dk.dbc.dataio.sink.marcconv.entity;

import dk.dbc.dataio.commons.conversion.ConversionParam;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "param")
@NamedQueries({
        @NamedQuery(
                name = StoredConversionParam.DELETE_CONVERSION_PARAM_QUERY_NAME,
                query = StoredConversionParam.DELETE_CONVERSION_PARAM_QUERY)
})
@NamedNativeQuery(
        name = StoredConversionParam.INSERT_CONVERSION_PARAM_QUERY_NAME,
        query = StoredConversionParam.INSERT_CONVERSION_PARAM_QUERY)
public class StoredConversionParam {
    public static final String DELETE_CONVERSION_PARAM_QUERY =
            "DELETE FROM StoredConversionParam param" +
                    " WHERE param.jobId = :jobId";
    public static final String DELETE_CONVERSION_PARAM_QUERY_NAME =
            "StoredConversionParam.delete";
    public static final String INSERT_CONVERSION_PARAM_QUERY =
            "INSERT INTO param (jobid, param) VALUES (?1, ?2)" +
                    " ON CONFLICT (jobid) DO NOTHING";
    public static final String INSERT_CONVERSION_PARAM_QUERY_NAME =
            "StoredConversionParam.insert";

    private static final ConversionParamConverter CONVERTER = new ConversionParamConverter();

    @Id
    private Integer jobId;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = ConversionParamConverter.class)
    private ConversionParam param;

    public StoredConversionParam() {
    }

    public StoredConversionParam(Integer jobId) {
        this.jobId = jobId;
    }

    public Integer getJobId() {
        return jobId;
    }

    public ConversionParam getParam() {
        return param;
    }

    public void setParam(ConversionParam param) {
        this.param = param;
    }

    /**
     * Stores the conversion parameters of a job, keeping those already stored
     * <p>
     * A single statement rather than a lookup followed by a write, since several threads
     * and several sink instances convert a job's items at once and each stores the
     * parameters of the first record it saw. Reading first would leave them racing to
     * insert the same row, and the loser's whole transaction, item conversion included,
     * would be aborted by the constraint violation.
     *
     * @return 1 when this call stored the parameters, 0 when the job already had some
     */
    public static int insertIfAbsent(EntityManager entityManager, int jobId, ConversionParam param) {
        return entityManager.createNamedQuery(INSERT_CONVERSION_PARAM_QUERY_NAME)
                .setParameter(1, jobId)
                .setParameter(2, CONVERTER.convertToDatabaseColumn(param))
                .executeUpdate();
    }

    @Override
    public String toString() {
        return "StoredConversionParam{" +
                "jobId=" + jobId +
                ", param=" + param +
                '}';
    }
}
