package hu.arnoldfarkas.flickruploader;

import java.io.File;

public interface FlickrUploader {

    void uploadPhotosToSet(File[] file, String setName);
}
