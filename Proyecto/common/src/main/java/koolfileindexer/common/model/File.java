package koolfileindexer.common.model;

import koolfileindexer.common.Constants;
import koolfileindexer.common.exceptions.InvalidFormatException;
import koolfileindexer.common.utils.FromStr;
import koolfileindexer.common.utils.IntoStr;

public class File implements IntoStr {

    private String name;
    private String extension;
    private String path;
    private String modifiedDate;
    private Integer size;
    private String[] tags;

    public File(
            String name,
            String extension,
            String path,
            String modifiedDate,
            Integer size,
            String[] tags) {
        this.name = name;
        this.extension = extension;
        this.path = path;
        this.modifiedDate = modifiedDate;
        this.size = size;
        this.tags = tags;
    }

    public String getName() {
        return name;
    }

    public String getExtension() {
        return extension;
    }

    public String getPath() {
        return path;
    }

    public String getModifiedDate() {
        return modifiedDate;
    }

    public Integer getSize() {
        return size;
    }

    public String[] getTags() {
        return tags;
    }

    public static FromStr<File> stringFactory() {
        return source -> {
            try {
                String[] lines = source.split(Constants.LINE_SEPARATOR);

                String name = lines[0].split(": ")[1];
                String extension = lines[1].split(": ")[1];
                String path = lines[2].split(": ")[1];
                String modifiedDate = lines[3].split(": ")[1];
                Integer size = Integer.parseInt(lines[4].split(": ")[1]);
                Integer tagsLength = Integer.parseInt(lines[5].split(": ")[1]);

                String[] tags = new String[tagsLength];

                for (int i = 0; i < tags.length; i++) {
                    tags[i] = lines[6 + i].split(": ", 2)[1];
                }

                return new File(name, extension, path, modifiedDate, size, tags);
            } catch (Exception e) {
                throw new InvalidFormatException(Search.class);
            }
        };
    }

    @Override
    public String intoString() {
        String result = "";
        result += "name: " + this.name + Constants.LINE_SEPARATOR;
        result += "extension: " + this.extension + Constants.LINE_SEPARATOR;
        result += "path: " + this.path + Constants.LINE_SEPARATOR;
        result += "modified-date: " + this.modifiedDate + Constants.LINE_SEPARATOR;
        result += "size: " + this.size + Constants.LINE_SEPARATOR;
        result += "tags-length: " + this.tags.length + Constants.LINE_SEPARATOR;
        for (String tag : this.tags) {
            result += "tag: " + tag + Constants.LINE_SEPARATOR;
        }
        return result;
    }
}
