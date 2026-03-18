package dk.dbc.dataio.harvester.infomedia;

import com.fasterxml.jackson.annotation.JsonInclude;
import dk.dbc.dataio.commons.creatordetector.connector.CreatorNameSuggestion;
import dk.dbc.dataio.commons.retriever.connector.model.Article;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArticlePayload {

    private Article article;
    private List<CreatorNameSuggestion> creatorNameSuggestions;

    public Article getArticle() {
        return article;
    }

    public void setArticle(Article article) {
        this.article = article;
    }

    public List<CreatorNameSuggestion> getCreatorNameSuggestions() {
        return creatorNameSuggestions;
    }

    public void setCreatorNameSuggestions(List<CreatorNameSuggestion> creatorNameSuggestions) {
        this.creatorNameSuggestions = creatorNameSuggestions;
    }
}
