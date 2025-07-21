package koolfileindexer.common.model;

import koolfileindexer.common.Constants;
import koolfileindexer.common.exceptions.InvalidFormatException;
import koolfileindexer.common.utils.FromStr;
import koolfileindexer.common.utils.IntoStr;

public class Search implements IntoStr {
    private final String[] keywords;
    private final String[] tags;
    private final String[] filters;

    public Search(String[] keywords, String[] tags, String[] filters) {
        this.keywords = keywords;
        this.tags = tags;
        this.filters = filters;
    }

    public String[] getKeywords() {
        return keywords;
    }

    public String[] getTags() {
        return tags;
    }

    public String[] getFilters() {
        return filters;
    }

    /**
     * Expects format:
     * keywords: {numner_of_keywords}
     * tags: {number_of_tags}
     * filters: {number_of_tags}
     * keyword: {keyword1}
     * keyword: {keyword2}
     * keyword: {keyword3}
     * tag: {tag1}
     * filter: {filter}
     * 
     * @return
     */
    public static FromStr<Search> stringFactory() {
        return source -> {
            try {
                String[] lines = source.split(Constants.LINE_SEPARATOR);

                Integer keywordsLength = Integer.parseInt(lines[0].split(": ")[1]);
                Integer tagsLength = Integer.parseInt(lines[1].split(": ")[1]);
                Integer filtersLength = Integer.parseInt(lines[2].split(": ")[1]);

                String[] keywords = new String[keywordsLength];
                String[] tags = new String[tagsLength];
                String[] filters = new String[filtersLength];

                for (int i = 0; i < keywords.length; i++) {
                    keywords[i] = lines[3 + i].split(": ", 2)[1];
                }
                for (int i = 0; i < tags.length; i++) {
                    tags[i] = lines[keywordsLength + 3 + i].split(": ", 2)[1];
                }
                for (int i = 0; i < filters.length; i++) {
                    filters[i] = lines[keywordsLength + tagsLength + 3 + i].split(": ", 2)[1];
                }

                return new Search(keywords, tags, filters);
            } catch (Exception e) {
                throw new InvalidFormatException(Search.class);
            }
        };
    }

    @Override
    public String intoString() {
        String result = "";
        result += "keywords-length: " + this.keywords.length + Constants.LINE_SEPARATOR;
        result += "tags-length: " + this.tags.length + Constants.LINE_SEPARATOR;
        result += "filters-length: " + this.filters.length + Constants.LINE_SEPARATOR;
        for (String keyword : this.keywords) {
            result += "keyword: " + keyword + Constants.LINE_SEPARATOR;
        }
        for (String tag : this.tags) {
            result += "tag: " + tag + Constants.LINE_SEPARATOR;
        }
        for (String filter : this.filters) {
            result += "filter: " + filter + Constants.LINE_SEPARATOR;
        }
        return result;
    }
}
