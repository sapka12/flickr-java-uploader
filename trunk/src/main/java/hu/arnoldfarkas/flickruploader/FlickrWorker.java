package hu.arnoldfarkas.flickruploader;

import java.io.File;

public interface FlickrWorker {

    void uploadPhotosToSet(File[] file, String setName, boolean familyOnly);
    
    void downloadPhotoSet(String setName, File outputFolder);
}
