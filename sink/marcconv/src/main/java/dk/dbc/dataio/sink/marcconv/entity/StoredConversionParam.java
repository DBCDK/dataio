package dk.dbc.dataio.sink.marcconv.entity;

import dk.dbc.dataio.commons.conversion.ConversionParam;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
public class StoredConversionParam {
    public static final String DELETE_CONVERSION_PARAM_QUERY =
            "DELETE FROM StoredConversionParam param" +
                    " WHERE param.jobId = :jobId";
    public static final String DELETE_CONVERSION_PARAM_QUERY_NAME =
            "StoredConversionParam.delete";

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

    @Override
    public String toString() {
        return "StoredConversionParam{" +
                "jobId=" + jobId +
                ", param=" + param +
                '}';
    }
}
