package hu.arnoldfarkas.flickruploader.flickr;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.RequestContext;
import com.flickr4java.flickr.Transport;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.auth.AuthInterface;
import com.flickr4java.flickr.auth.Permission;
import com.flickr4java.flickr.photosets.Photoset;
import com.flickr4java.flickr.photosets.Photosets;
import com.flickr4java.flickr.uploader.UploadMetaData;
import hu.arnoldfarkas.flickruploader.util.Utils;
import hu.arnoldfarkas.flickruploader.util.Utils;
import java.io.File;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlickrHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlickrHelper.class);
    private static final String API_KEY = "17b52840f55eba355a4c3d20c128430d";
    private static final String SECRET = "d0fccf78386ca7ed";
    private static final Transport TRANSPORT = new REST();
    Flickr FLICKR = new Flickr(API_KEY, SECRET, TRANSPORT);
    private AuthInterface authInterface;
    private Token authtoken;
    private Token requestToken;

    public FlickrHelper() {
        Flickr.debugStream = false;
        initAuthInterface();
    }

    private void initAuthInterface() {
        authInterface = FLICKR.getAuthInterface();
    }

    public String getAuthorizationUrl() {
        authtoken = authInterface.getRequestToken();
        LOGGER.debug("authtoken: " + authtoken);
        return authInterface.getAuthorizationUrl(authtoken, Permission.WRITE);
    }

    public Token initRequestToken(String tokenKey) {
        requestToken = authInterface.getAccessToken(authtoken, new Verifier(tokenKey));
        LOGGER.info("requestToken secret: {}" , requestToken.getSecret());
        LOGGER.info("requestToken token: {}" , requestToken.getToken());
        return requestToken;
    }
    public Auth getValidatedUser() throws FlickrException {
        Auth auth = authInterface.checkToken(requestToken);
        validateAuth(auth);
        return auth;
    }

    public void uploadPhotoToSet(final File file, final String setName) throws FlickrException {
        if (!isJpg(file)) {
            return;
        }

        Runnable job = new Runnable() {
            public void run() {
                try {
                    LOGGER.debug("uploading photo to " + setName + ": " + file.getName());
                    String photoId = uploadPhoto(file);
                    LOGGER.debug("uploaded photo[" + file.getName() + "] id: " + photoId);
                    putPhotoToSet(photoId, setName);
                } catch (FlickrException e) {
                    LOGGER.error("error on upload photo to set", e);
                }
            }
        };
        Utils.addJobToExecutor(job);
    }

    private boolean isJpg(File file) {
        return file.getAbsolutePath().toLowerCase().endsWith(".jpg");
    }

    private String uploadPhoto(File file) throws FlickrException {
        UploadMetaData metaData = new UploadMetaData();
        metaData.setTitle(file.getName());
        getValidatedUser();
        String photoId = FLICKR.getUploader().upload(file, metaData);
        return photoId;
    }

    private void putPhotoToSet(String photoId, String setName) throws FlickrException {
        String setId = getSetIdByName(setName);
        if (setId == null) {
            createSetWithDefaultPicture(setName, photoId);
        } else {
            FLICKR.getPhotosetsInterface().addPhoto(setId, photoId);
        }
    }

    private synchronized void createSetWithDefaultPicture(String setName, String primaryPhotoId) throws FlickrException {
        String title = setName;
        String description = setName;
        Photoset photoset = FLICKR.getPhotosetsInterface().create(title, description, primaryPhotoId);
        LOGGER.debug("Set created: " + photoset.getId() + ", " + photoset.getTitle() + ", " + photoset.getDescription());
    }

    private String getSetIdByName(String setName) throws FlickrException {
        Photosets photosets = FLICKR.getPhotosetsInterface().getList(getValidatedUser().getUser().getId());
        for (Photoset photoset : photosets.getPhotosets()) {
            if (photoset.getTitle().equals(setName)) {
                return photoset.getId();
            }
        }
        return null;
    }

    private void validateAuth(Auth auth) {
        RequestContext.getRequestContext().setAuth(auth);
    }
}
