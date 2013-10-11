package hu.arnoldfarkas.flickruploader;

import hu.arnoldfarkas.flickruploader.flickr.FlickrFolderFinder;
import hu.arnoldfarkas.flickruploader.flickr.FlickrFolderInfo;
import hu.arnoldfarkas.flickruploader.flickr.FlickrRequestTokenFinder;
import hu.arnoldfarkas.flickruploader.flickr.FlickrUploaderImpl;
import hu.arnoldfarkas.flickruploader.util.Utils;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskChooser {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskChooser.class);
    private static final String ARG_FIND_TOKEN = "findtoken";
    private static final String ARG_BATCH_UPLOAD = "batchupload";
    private static final String ARG_DOWNLOAD = "downloadsettofolder";
    
    public void execute(String[] args) {
        if (args == null || args.length < 1) {
            printHelp();
        } else if (args[0].toLowerCase().equals(ARG_FIND_TOKEN)) {
            findToken();
        } else if (args[0].toLowerCase().equals(ARG_BATCH_UPLOAD) && args.length > 1) {
            batchUpload(args[1]);
        } else if (args[0].toLowerCase().equals(ARG_DOWNLOAD) && args.length > 2) {
            download(args[1], args[2]);
        } else {
            printHelp();
        }
    }

    private void printHelp() {
        System.out.println("HELP:");
        System.out.println();
        System.out.println("--------------------------------------");
        System.out.println();
        System.out.println("agrs[0]: " + arg0Optins());
        System.out.println();
        System.out.println(ARG_FIND_TOKEN);
        System.out.println(ARG_BATCH_UPLOAD + " 'basefolderpath'");
        System.out.println(ARG_DOWNLOAD + " 'fromsetname' 'tofolderpath'");
        
    }

    private String arg0Optins() {
        StringBuilder builder = new StringBuilder();
        builder.append(ARG_FIND_TOKEN);
        builder.append(", ");
        builder.append(ARG_BATCH_UPLOAD);
        builder.append(", ");
        builder.append(ARG_DOWNLOAD);

        return builder.toString();
    }

    private void findToken() {
        LOGGER.debug(ARG_FIND_TOKEN);
        FlickrRequestTokenFinder tokenFinder = new FlickrRequestTokenFinder();
        LOGGER.debug(tokenFinder.find().toString());
    }

    private void batchUpload(String baseFolderPath) {
        LOGGER.debug(ARG_BATCH_UPLOAD);
        LOGGER.debug("FileUploader running on {}", baseFolderPath);
        File baseFolder = new File(baseFolderPath);

        FlickrFolderFinder finder = new FlickrFolderFinder(baseFolder);
        for (FlickrFolderInfo folderInfo : finder.find()) {
            folderInfo.update();
        }
        LOGGER.debug("FileUploader ended {}", baseFolderPath);
    }

    private void download(String setName, String toFolderPath) {
        FlickrWorker worker = new FlickrUploaderImpl();
        worker.downloadPhotoSet(setName, new File(toFolderPath));
    }
}
