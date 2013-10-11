package hu.arnoldfarkas.flickruploader.flickr;

import hu.arnoldfarkas.flickruploader.FlickrUploader;
import hu.arnoldfarkas.flickruploader.util.FileUploadMarker;
import hu.arnoldfarkas.flickruploader.util.Utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlickrFolderInfo {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlickrFolderInfo.class);
    public static final String FILENAME = "flickr.info.properties";
    public static final String PROP_SET = "set";
    public static final String PROP_UPLOADED = "uploaded";
    public static final String PROP_UPLOADED_FILENAMES = "uploaded.filenames";
    public static final FlickrUploader UPLOADER = new FlickrUploaderImpl();
    private Properties properties;
    private File folder;

    private FlickrFolderInfo(File flickrFolder) throws IOException {
        folder = flickrFolder;
        properties = new Properties();

        String propertyFileAbsPath = getPropertyFilePath();
        LOGGER.trace("propertyFileAbsPath: {}", propertyFileAbsPath);
        properties.load(new FileInputStream(propertyFileAbsPath));
    }

    private String getPropertyFilePath() {
        return folder.getAbsolutePath() + "/" + FILENAME;
    }

    public static FlickrFolderInfo load(File flickrFolder) throws FileNotFoundException, IOException {
        return new FlickrFolderInfo(flickrFolder);
    }

    public void update() {

        String setName = properties.getProperty(PROP_SET);
        boolean uploaded = properties.getProperty(PROP_UPLOADED) == null ? false : properties.getProperty(PROP_UPLOADED).toLowerCase().equals("true");

        if (uploaded) {
            LOGGER.debug("Already uploaded: {}, {}", folder.getAbsolutePath(), properties);
        } else if (setName == null) {
            LOGGER.debug("Folder info has no setname: {}", folder.getAbsolutePath());
        } else {
            LOGGER.info("Uploading: {}, {}", folder.getAbsolutePath(), properties);
            UPLOADER.uploadPhotosToSet(Utils.jpgFilesInDirectory(folder), setName);
            refreshProperties();
        }
    }

    private void refreshProperties() {
        properties.setProperty(PROP_UPLOADED, "true");
        String path = getPropertyFilePath();
        try {
            properties.store(new FileOutputStream(path), Calendar.getInstance().toString());
        } catch (Throwable ex) {
            LOGGER.error("Cannot save properties file: {}", path);
        }
    }
}
