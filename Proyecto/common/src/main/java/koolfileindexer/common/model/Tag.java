package koolfileindexer.common.model;

import koolfileindexer.common.Constants;
import koolfileindexer.common.exceptions.InvalidFormatException;
import koolfileindexer.common.utils.FromStr;
import koolfileindexer.common.utils.IntoStr;

public class Tag implements IntoStr {

    private String name;

    public Tag(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static FromStr<Tag> stringFactory() {
        return source -> {
            try {
                String[] lines = source.split(Constants.LINE_SEPARATOR);

                String tag = lines[0].split(": ")[1];

                return new Tag(tag);
            } catch (Exception e) {
                throw new InvalidFormatException(Search.class);
            }
        };
    }

    @Override
    public String intoString() {
        String result = "";
        result += "tag: " + this.name + Constants.LINE_SEPARATOR;
        return result;
    }
}
