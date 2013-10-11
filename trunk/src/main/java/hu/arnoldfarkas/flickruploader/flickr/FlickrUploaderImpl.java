package hu.arnoldfarkas.flickruploader.flickr;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.RequestContext;
import com.flickr4java.flickr.Transport;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.photosets.Photoset;
import com.flickr4java.flickr.photosets.Photosets;
import com.flickr4java.flickr.photosets.PhotosetsInterface;
import com.flickr4java.flickr.uploader.UploadMetaData;
import com.flickr4java.flickr.uploader.Uploader;
import hu.arnoldfarkas.flickruploader.FlickrUploader;
import hu.arnoldfarkas.flickruploader.util.FileUploadMarker;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import org.scribe.model.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlickrUploaderImpl implements FlickrUploader {

    private static final Properties PROPERTIES = loadProperties();
    private static final Logger LOGGER = LoggerFactory.getLogger(FlickrUploaderImpl.class);
    private static final String API_KEY = PROPERTIES.getProperty("flickr.apikey");
    private static final String SECRET = PROPERTIES.getProperty("flickr.apisecret");
    private static final Transport TRANSPORT = new REST();
    private static final Flickr FLICKR = new Flickr(API_KEY, SECRET, TRANSPORT);
    private static final Token REQUEST_TOKEN = new Token(
      PROPERTIES.getProperty("flickr.requesttoken.token"),
      PROPERTIES.getProperty("flickr.requesttoken.secret"));

    public FlickrUploaderImpl() {
        LOGGER.debug("FLICKR PROPERTIES: {}", PROPERTIES);
    }

    private static Properties loadProperties() {
        try {
            Properties p = new Properties();
            InputStream is = new FileInputStream("flickr.properties");
            p.load(is);
            return p;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void uploadPhotosToSet(File[] files, final String setName) {
        files = notMarked(files);
        if (files == null || files.length < 1) {
            return;
        }

        File firstPhoto = files[0];
        String firstPhotoId = uploadPhoto(firstPhoto);

        Photoset photoset = findPhotosetByName(setName);
        if (photoset == null) {
            photoset = createSetWithDefaultPicture(setName, firstPhotoId);
        } else {
            try {
                putPhotoToSet(firstPhotoId, photoset);
            } catch (FlickrException ex) {
                throw new RuntimeException(ex);
            }
        }

        if (files.length < 2) {
            return;
        }
        final File[] notFirstPhotos = Arrays.copyOfRange(files, 1, files.length);
        for (File file : notFirstPhotos) {
            uploadPhotoToSet(file, photoset);
        }
    }

    private void uploadPhotoToSet(final File photo, final Photoset photoset) {
        try {
            String photoId = uploadPhoto(photo);
            putPhotoToSet(photoId, photoset);
        } catch (FlickrException ex) {
            LOGGER.error("Error on upload: {}", photo.getName(), ex);
        }
    }

    private void putPhotoToSet(String photoId, Photoset photoset) throws FlickrException {
        FLICKR.getPhotosetsInterface().addPhoto(photoset.getId(), photoId);
        LOGGER.debug("Photo [{}] added to set: {}", photoId, photoset.getTitle());
    }

    private Photoset createSetWithDefaultPicture(String setName, String primaryPhotoId) {
        String title = setName;
        String description = setName;
        Photoset photoset = createSetWithDefaultPictureWithException(title, description, primaryPhotoId);
        LOGGER.debug("Set created: " + photoset.getId() + ", " + photoset.getTitle() + ", " + photoset.getDescription());
        return photoset;
    }

    private String uploadPhoto(File file) {
        UploadMetaData metaData = new UploadMetaData();
        metaData.setTitle(file.getName());
        metaData.setHidden(false);
        metaData.setPublicFlag(true);
        validateAuth(getAuth());
        String photoId = uploadPhotoWithException(file, metaData);
        LOGGER.debug("Photo uploaded: {}", file.getAbsolutePath());
        markAsUploaded(file);
        return photoId;
    }

    public Auth getAuth() {
        try {
            return FLICKR.getAuthInterface().checkToken(REQUEST_TOKEN);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void validateAuth(Auth auth) {
        RequestContext.getRequestContext().setAuth(auth);
    }

    private String uploadPhotoWithException(File file, UploadMetaData metaData) {
        Uploader uploader = FLICKR.getUploader();
        try {
            return uploader.upload(file, metaData);
        } catch (FlickrException ex) {
            LOGGER.warn("upload 2nd try needed: {}", file.getAbsolutePath(), ex);
            waiting();
            try {
                return uploader.upload(file, metaData);
            } catch (FlickrException ex2) {
                LOGGER.warn("upload 3rd try needed: {}", file.getAbsolutePath(), ex2);
                waiting();
                try {
                    return uploader.upload(file, metaData);
                } catch (FlickrException ex3) {
                    LOGGER.error("upload failed: {}", file.getAbsolutePath(), ex3);
                    throw new RuntimeException(ex3);
                }
            }
        }
    }

    private void waiting() {
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException ex) {
            LOGGER.debug("sleep interrupted", ex);
        }
    }

    private Photoset createSetWithDefaultPictureWithException(String title, String description, String primaryPhotoId) {
        PhotosetsInterface psi = FLICKR.getPhotosetsInterface();
        try {
            return psi.create(title, description, primaryPhotoId);
        } catch (FlickrException ex) {
            LOGGER.warn("set creating 2nd try needed: {}", title, ex);
            waiting();
            try {
                return psi.create(title, description, primaryPhotoId);
            } catch (FlickrException ex2) {
                LOGGER.warn("set creating 3rd try needed: {}", title, ex2);
                waiting();
                try {
                    return psi.create(title, description, primaryPhotoId);
                } catch (FlickrException ex3) {
                    LOGGER.warn("set creating failed: {}", title, ex3);
                    throw new RuntimeException(ex3);
                }
            }
        }
    }

    private Photoset findPhotosetByName(String setName) {
        try {
            final Photosets photosets = FLICKR.getPhotosetsInterface().getList(getAuth().getUser().getId());
            for (Photoset photoset : photosets.getPhotosets()) {
                if (photoset.getTitle().equals(setName)) {
                    return photoset;
                }
            }
        } catch (Throwable e) {
            LOGGER.warn("Cannot find set: {}", setName, e);
        }
        return null;
    }

    private File[] notMarked(File[] files) {
        Set<File> notMarkedFiles = new HashSet<File>();
        for (File file : files) {
            FileUploadMarker marker = new FileUploadMarker(file);
            if (!marker.isMarked()) {
                notMarkedFiles.add(file);
            }
        }
        files = new File[notMarkedFiles.size()];
        int i = 0;
        for (File file : notMarkedFiles) {
            files[i] = file;
            i++;
        }
        return files;
    }

    private void markAsUploaded(File file) {
        FileUploadMarker marker = new FileUploadMarker(file);
        marker.mark();
    }
}
