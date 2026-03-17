package dk.dbc.dataio.commons.retriever.connector.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

/**
 * This immutable record encapsulates all parameters needed to query and retrieve articles from a data source.
 * It supports date range filtering, text-based searching, pagination, and formatting options for the response.
 * <p>
 * All parameters are optional (nullable) to allow flexible querying. Date parameters are formatted as
 * "yyyy-MM-dd" strings during JSON serialization and deserialization. Null fields are excluded from
 * JSON output.
 * <p>
 * The class provides a builder pattern through its nested Builder class to facilitate construction
 * of request objects with only the desired parameters set.
 * <p>
 * Thread-safety: This record is immutable and thread-safe.
 *
 * @param fromDate the start date for filtering articles (inclusive), formatted as "yyyy-MM-dd"
 * @param toDate the end date for filtering articles (exclusive), formatted as "yyyy-MM-dd"
 * @param query the search query string for filtering articles by content
 * @param page the page number for pagination (one-based)
 * @param size the number of articles to return per page
 * @param formatFulltextHtml whether to format the full text content as HTML in the response
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ArticlesRequest(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate fromDate,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate toDate,
        String query,
        Integer page,
        Integer size,
        Boolean formatFulltextHtml
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LocalDate fromDate;
        private LocalDate toDate;
        private String query;
        private Integer page;
        private Integer size;
        private Boolean formatFulltextHtml;

        public Builder fromDate(LocalDate fromDate) {
            this.fromDate = fromDate;
            return this;
        }

        public Builder toDate(LocalDate toDate) {
            this.toDate = toDate;
            return this;
        }

        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder page(Integer page) {
            this.page = page;
            return this;
        }

        public Builder size(Integer size) {
            this.size = size;
            return this;
        }

        public Builder formatFulltextHtml(Boolean formatFulltextHtml) {
            this.formatFulltextHtml = formatFulltextHtml;
            return this;
        }

        public ArticlesRequest build() {
            return new ArticlesRequest(fromDate, toDate, query, page, size, formatFulltextHtml);
        }
    }
}
