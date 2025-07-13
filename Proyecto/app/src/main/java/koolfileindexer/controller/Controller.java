package koolfileindexer.controller;

import java.util.ArrayList;
import java.util.List;

public class Controller {

    public Controller() {}

    public List<File> searchFiles(String searchTerms)
        throws UninitializedServerException {
        // TODO: Parse searchTerms
        List<File> files = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            files.add(File.getMockupInstance(searchTerms + i));
            if (i == 70) {
                throw new UninitializedServerException(555);
            }
        }

        return files;
    }
}
