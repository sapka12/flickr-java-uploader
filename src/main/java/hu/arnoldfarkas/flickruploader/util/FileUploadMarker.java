package hu.arnoldfarkas.flickruploader.util;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUploadMarker {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUploadMarker.class);
    public static final String CHARSET_UTF8 = "utf-8";
    private File markerFile;
    private File file;
    private static final String MARKER_FILENAME = "flickr.marker.txt";

    public FileUploadMarker(File file) {
        assertFile(file);
        String filePath = file.getParent() + "/" + MARKER_FILENAME;
        LOGGER.trace("FileUploadMarker Path: {}", filePath);
        this.file = file;
        this.markerFile = new File(filePath);
    }

    private void assertFile(File file) {
        if (file == null) {
            throw new IllegalArgumentException("File have to be not NULL.");
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("File have to be FILE: " + file.getAbsolutePath());
        }
    }

    public boolean isMarked() {
        if (!markerFile.exists()) {
            return false;
        }
        if (!containsFileName()) {
            return false;
        }
        return true;
    }

    private boolean containsFileName() {
        List<String> lines;
        try {
            lines = FileUtils.readLines(markerFile);
        } catch (IOException ex) {
            LOGGER.warn("error containsMarkerFileName", ex);
            return false;
        }
        for (String line : lines) {
            if (line.equals(file.getName())) {
                return true;
            }
        }
        return false;
    }

    public void mark() {
        createMarkerFileIfNotExists();
        addFilenameAsNewLine();
    }

    private void createMarkerFileIfNotExists() {
        if (!markerFile.exists()) {
            try {
                markerFile.createNewFile();
            } catch (IOException ex) {
                LOGGER.warn("Cannot create file: {}", markerFile.getAbsolutePath(), ex);
            }
        }
    }

    private void addFilenameAsNewLine() {
        try {
            List<String> lines = FileUtils.readLines(markerFile);
            lines.add(file.getName());
            FileUtils.writeLines(markerFile, lines);
        } catch (IOException ex) {
            LOGGER.error("Cannot write file: {}", markerFile.getAbsolutePath(), ex);
        }
    }
}
