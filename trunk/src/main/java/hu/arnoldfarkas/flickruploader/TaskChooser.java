package hu.arnoldfarkas.flickruploader;

import hu.arnoldfarkas.flickruploader.flickr.FlickrFolderFinder;
import hu.arnoldfarkas.flickruploader.flickr.FlickrFolderInfo;
import hu.arnoldfarkas.flickruploader.flickr.FlickrRequestTokenFinder;
import hu.arnoldfarkas.flickruploader.flickr.FlickrUploaderImpl;
import java.io.File;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskChooser {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskChooser.class);
    private static final String ARG_FIND_TOKEN = "findtoken";
    private static final String ARG_FIND_TOKEN_SHORT = "f";
    private static final String ARG_BATCH_UPLOAD = "batchupload";
    private static final String ARG_BATCH_UPLOAD_SHORT = "u";
    private static final String ARG_DOWNLOAD = "downloadsettofolder";
    private static final String ARG_DOWNLOAD_SHORT = "d";

    private static final String ARG_FAMILY = "family";
    private static final String ARG_SET = "set";
    private static final String ARG_FOLDER = "folder";

    private CommandLine commandLine;

    public void execute(String[] args) {
        commandLine = parse(args);
        if (commandLine.hasOption(ARG_FIND_TOKEN)) {
            findToken();
        } else if (commandLine.hasOption(ARG_BATCH_UPLOAD) && commandLine.hasOption(ARG_FOLDER)) {
            batchUpload(commandLine.getOptionValue(ARG_FOLDER), isFamilyOnly());
        } else if (commandLine.hasOption(ARG_DOWNLOAD) && commandLine.hasOption(ARG_FOLDER) && commandLine.hasOption(ARG_SET)) {
            download(commandLine.getOptionValue(ARG_SET), commandLine.getOptionValue(ARG_FOLDER));
        } else {
            printHelp();
        }
    }

    private void findToken() {
        LOGGER.debug(ARG_FIND_TOKEN);
        FlickrRequestTokenFinder tokenFinder = new FlickrRequestTokenFinder();
        LOGGER.debug(tokenFinder.find().toString());
    }

    private void batchUpload(String baseFolderPath, boolean familyOnly) {
        LOGGER.debug(ARG_BATCH_UPLOAD);
        LOGGER.debug("FileUploader running on {}", baseFolderPath);
        File baseFolder = new File(baseFolderPath);

        FlickrFolderFinder finder = new FlickrFolderFinder(baseFolder);
        for (FlickrFolderInfo folderInfo : finder.find()) {
            folderInfo.update(familyOnly);
        }
        LOGGER.debug("FileUploader ended {}", baseFolderPath);
    }

    private void download(String setName, String toFolderPath) {
        FlickrWorker worker = new FlickrUploaderImpl();
        worker.downloadPhotoSet(setName, new File(toFolderPath));
    }

    private CommandLine parse(String[] args) {
        CommandLineParser parser = new BasicParser();
        try {
            return parser.parse(createOptions(), args);
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Options createOptions() {
        Options options = new Options();
        options.addOption(ARG_DOWNLOAD_SHORT, ARG_DOWNLOAD, false, ARG_DOWNLOAD);
        options.addOption(ARG_BATCH_UPLOAD_SHORT, ARG_BATCH_UPLOAD, false, ARG_BATCH_UPLOAD);
        options.addOption(ARG_FIND_TOKEN_SHORT, ARG_FIND_TOKEN, false, ARG_FIND_TOKEN);

        options.addOption(ARG_SET, true, ARG_SET);
        options.addOption(ARG_FOLDER, true, ARG_FOLDER);
        options.addOption(ARG_FAMILY, false, ARG_FAMILY);
        return options;
    }

    private void printHelp() {
        for (Object o : createOptions().getOptions()) {
            if (o instanceof Option) {
                Option option = (Option) o;
                System.out.println(option);
            }
        }
    }

    private boolean isFamilyOnly() {
        return commandLine.hasOption(ARG_FAMILY);
    }

}
