package koolfileindexer.controller;

public class File {

    private String name;
    private String extension;
    private String path;
    private String modifiedDate;
    private Integer size;
    private String[] tags;

    // Constructor is private for mockup reasons
    private File() {
        this.name = "file1";
        this.extension = "txt";
        this.path = "/home/user/file1.txt";
        this.modifiedDate = "12/2/2002";
        this.size = 1820;
        this.tags = new String[] { "Plain Text Document", "Vacations" };
    }

    public File(
        String name,
        String extension,
        String path,
        String modifiedDate,
        Integer size,
        String[] tags
    ) {
        this.name = name;
        this.extension = extension;
        this.path = path;
        this.modifiedDate = modifiedDate;
        this.size = size;
        this.tags = tags;
    }

    public static File getMockupInstance() {
        return new File();
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
}
