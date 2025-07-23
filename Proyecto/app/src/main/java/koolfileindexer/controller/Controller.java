package koolfileindexer.controller;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;

import koolfileindexer.common.Constants;
import koolfileindexer.common.model.ErrorMessage;
import koolfileindexer.common.model.File;
import koolfileindexer.common.model.GenericList;
import koolfileindexer.common.model.Search;
import koolfileindexer.common.protocol.Request;
import koolfileindexer.common.protocol.Response;
import koolfileindexer.common.protocol.v1.Socket;
import koolfileindexer.common.utils.Result;

public class Controller {

    public Set<String> excludedFolders;

    public Controller() {
        this.excludedFolders = new HashSet<>();
        this.addProtectedFolders();
    }

    private void addProtectedFolders() {
        this.excludedFolders.add("/proc");
        this.excludedFolders.add("/sys");
        this.excludedFolders.add("/dev");
    }

    public List<File> searchFiles(String searchTerms)
            throws UninitializedServerException {
        String[] terms = searchTerms.split(" ");
        List<String> filters = new ArrayList<>();
        List<String> keywords = new ArrayList<>();
        List<String> tags = new ArrayList<>();

        for (String term : terms) {
            if (term.startsWith("tag:")) {
                tags.add(term.substring(4));
                continue;
            }
            if (term.startsWith("keyword:")) {
                keywords.add(term.substring(8));
            }
            filters.add("name:" + term);
        }

        try {
            Socket socket = Socket.connect(
                    Constants.PROTOCOL_HEADER,
                    Constants.PROTOCOL_TRAILER);

            socket.sendRequest(
                    new Request(
                            "search",
                            new Search(
                                    keywords.toArray(String[]::new),
                                    tags.toArray(String[]::new),
                                    filters.toArray(String[]::new))));
            Response res = socket.getResponse();

            Result<GenericList<File>, ErrorMessage> files = res.getData(
                    GenericList.stringFactory(File.stringFactory()));

            socket.close();

            return switch (files) {
                case Result.Success<GenericList<File>, ErrorMessage> s -> {
                    System.out.println("I have " + s.value().size());
                    yield s.value();
                }

                case Result.Error<GenericList<File>, ErrorMessage> e -> {
                    System.out.println("Got Error: " + e.error().getErrorMessage());
                    yield new ArrayList<>();
                }
            };
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return new ArrayList<>();
    }

    public void setExcludedFiles(Iterator<String> iterator) {
        Set<String> excludedFiles = new HashSet<>();

        while (iterator.hasNext()) {
            String s = iterator.next();
            if (s.trim().isEmpty()) {
                continue;
            }
            excludedFiles.add(s.trim());
        }

        this.excludedFolders = excludedFiles;
        this.addProtectedFolders();
        this.writeExcludedFoldersToConfig();
    }

    /**
     * Writes the contents of excludedFolders to
     * /.config/koolfileindexer/excludedfolders.txt,
     * creating directories and file if needed.
     */
    public void writeExcludedFoldersToConfig() {
        String home = System.getenv("HOME");
        if (home == null || home.isEmpty()) {
            throw new RuntimeException("HOME environment variable not set");
        }
        java.nio.file.Path configDir = java.nio.file.Paths.get(home, ".config", "koolfileindexer");
        java.nio.file.Path configFile = configDir.resolve("exclusiones.txt");
        try {
            java.nio.file.Files.createDirectories(configDir);
            java.nio.file.Files.write(
                    configFile,
                    this.excludedFolders,
                    java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to write excluded folders config: " + e.getMessage(), e);
        }
    }

    public void setKeyword(File file, String keyword) {
        try {
            Socket socket = Controller.getSocket();
            socket.sendRequest(
                    new Request("addKeyword", "keyword: " + keyword + Constants.LINE_SEPARATOR + "file-path: " + file
                            .getPath() + Constants.LINE_SEPARATOR));

            Response res = socket.getResponse();
            Result<String, ErrorMessage> result = res.getData(source -> {
                return source;
            });

            switch (result) {
                case Result.Success<String, ErrorMessage> s -> {
                    System.out.println(s.value());
                }
                case Result.Error<String, ErrorMessage> e -> {
                    System.out.println(e.error().getErrorMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setTag(File file, String tag) {
        try {
            Socket socket = Controller.getSocket();
            socket.sendRequest(
                    new Request("addTag", "tag: " + tag + Constants.LINE_SEPARATOR + "file-path: " + file
                            .getPath() + Constants.LINE_SEPARATOR));

            Response res = socket.getResponse();
            Result<String, ErrorMessage> result = res.getData(source -> {
                return source;
            });

            switch (result) {
                case Result.Success<String, ErrorMessage> s -> {
                    System.out.println(s.value());
                }
                case Result.Error<String, ErrorMessage> e -> {
                    System.out.println(e.error().getErrorMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Socket getSocket() throws SocketException, IOException {
        return Socket.connect(Constants.PROTOCOL_HEADER, Constants.PROTOCOL_TRAILER);
    }
}
