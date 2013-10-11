package hu.arnoldfarkas.flickruploader.flickr;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.RequestContext;
import com.flickr4java.flickr.Transport;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.PhotoList;
import com.flickr4java.flickr.photos.Size;
import com.flickr4java.flickr.photosets.Photoset;
import com.flickr4java.flickr.photosets.Photosets;
import com.flickr4java.flickr.photosets.PhotosetsInterface;
import com.flickr4java.flickr.uploader.UploadMetaData;
import com.flickr4java.flickr.uploader.Uploader;
import hu.arnoldfarkas.flickruploader.FlickrWorker;
import hu.arnoldfarkas.flickruploader.util.FileUploadMarker;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;
import org.scribe.model.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlickrUploaderImpl implements FlickrWorker {

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

    @Override
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

    private Auth getAuth() {
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
        while (true) {
            try {
                return uploader.upload(file, metaData);
            } catch (FlickrException ex) {
                LOGGER.warn("needed try upload again: {}", file.getAbsolutePath(), ex);
            }
        }
    }

    private Photoset createSetWithDefaultPictureWithException(String title, String description, String primaryPhotoId) {
        PhotosetsInterface psi = FLICKR.getPhotosetsInterface();
        while (true) {
            try {
                return psi.create(title, description, primaryPhotoId);
            } catch (FlickrException ex) {
                LOGGER.warn("need try again set creating : {}", title, ex);
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

    @Override
    public void downloadPhotoSet(String setName, File outputFolder) {

        if (outputFolder.exists()) {
            if (!outputFolder.isDirectory()) {
                LOGGER.warn("{} must be folder", outputFolder.getAbsolutePath());
                return;
            }
        } else {
            outputFolder.mkdir();
        }

        Photoset photoset = findPhotosetByName(setName);
        if (photoset == null) {
            LOGGER.warn("Set not found: ", setName);
            return;
        }

        List<Photo> photos = getPhotos(setName);


        long startTime = System.currentTimeMillis();
        int numberOfPhotos = photos.size();
        long actualIndex = 0;
        for (Photo photo : photos) {
            actualIndex++;
            uploadPhotoToDir(photo, outputFolder);
            long actualTime = System.currentTimeMillis();
            Date estimatedEnd = estimatedEnd(startTime, actualTime, actualIndex, numberOfPhotos);
            LOGGER.debug("Estimated end date: {}", estimatedEnd);
        }

    }

    private List<Photo> getPhotos(String setName) {
        try {
            int perPage = Integer.MAX_VALUE;
            int page = 0;
            final PhotoList<Photo> photos = FLICKR.getPhotosetsInterface().getPhotos(findPhotosetByName(setName).getId(), perPage, page);
            List<Photo> photoSublist = photos.subList(0, photos.size());
            return photoSublist;
        } catch (FlickrException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void uploadPhotoToDirWithException(Photo photo, File outputFolder) throws FlickrException, IOException {
        InputStream is = FLICKR.getPhotosInterface().getImageAsStream(photo, Size.ORIGINAL);
        File newPhotofile = createDownloadedFilePath(outputFolder, photo.getTitle());
        FileUtils.copyInputStreamToFile(is, newPhotofile);
        LOGGER.debug("{} downloaded", newPhotofile.getAbsolutePath());
    }

    private void uploadPhotoToDir(Photo photo, File outputFolder) {
        boolean downloaded = false;
        while (!downloaded) {
            try {
                uploadPhotoToDirWithException(photo, outputFolder);
                downloaded = true;
            } catch (Throwable e) {
                LOGGER.debug("need to try download again: {}", photo.getTitle(), e);
            }
        }
    }

    private String generateDownloadedFilePath(File outputFolder, String title) {
        String filePath = outputFolder.getAbsolutePath() + "/" + title;
        if (!filePath.toLowerCase().endsWith(".jpg")) {
            filePath += ".jpg";
        }
        return filePath;
    }

    private File createDownloadedFilePath(File outputFolder, String title) {
        return new File(generateDownloadedFilePath(outputFolder, title));
    }

    private Date estimatedEnd(long startTime, long actualTime, long idx, int all) {
        double rate = (1.0 * idx) / all;
        long timeSpent = actualTime - startTime;
        Double estimatedTimeSpent = timeSpent / rate;
        return new Date(startTime + estimatedTimeSpent.longValue());
    }
}
