package hu.arnoldfarkas.flickruploader.flickr;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlickrFolderFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlickrFolderFinder.class);
    private final File baseFolder;
    private static final FileFilter FILE_FILTER_DIRECTORY = new FileFilter() {
        public boolean accept(File file) {
            return file.isDirectory();
        }
    };

    public FlickrFolderFinder(File baseFolder) {
        if (!baseFolder.isDirectory()) {
            LOGGER.warn("Basefolder must be a folder: {}", baseFolder.getAbsolutePath());
            throw new RuntimeException("Basefolder must be a folder: " + baseFolder.getAbsolutePath());
        }
        this.baseFolder = baseFolder;
    }

    public Set<FlickrFolderInfo> find() {
        Set<FlickrFolderInfo> infos = new HashSet<FlickrFolderInfo>();
        for (File flickrFolder : flickrInfoContainingFolders()) {
            try {
                FlickrFolderInfo info = FlickrFolderInfo.load(flickrFolder);
                infos.add(info);
            } catch (IOException e) {
                LOGGER.warn("Error while loading flickr folder: {}", flickrFolder.getAbsolutePath(), e);
            }
        }
        return infos;
    }

    private Iterable<File> flickrInfoContainingFolders() {
        Set<File> flickrFolders = new HashSet<File>();
        
        if (isCointainsFlickrInfo(baseFolder)) {
            flickrFolders.add(baseFolder);
        }

        for (File folder : getSubFolders(baseFolder)) {
            if (isCointainsFlickrInfo(folder)) {
                LOGGER.info("CointainsFlickrInfo: {}", folder.getAbsolutePath());
                flickrFolders.add(folder);
            }
        }
        
        return flickrFolders;
    }

    private Set<File> getSubFolders(File folder) {
        File[] folderArray = folder.listFiles(FILE_FILTER_DIRECTORY);
        Set<File> folders = new HashSet<File>(Arrays.asList(folderArray));
        for (File subFolder : folderArray) {
            folders.addAll(getSubFolders(subFolder));
        }
        return folders;
    }

    private boolean isCointainsFlickrInfo(File folder) {
        return folder.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return FlickrFolderInfo.FILENAME.equals(name);
            }
        }).length == 1;
    }
}
