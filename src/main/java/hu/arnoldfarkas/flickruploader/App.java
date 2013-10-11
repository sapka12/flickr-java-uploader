package hu.arnoldfarkas.flickruploader;

import hu.arnoldfarkas.flickruploader.flickr.FlickrFolderFinder;
import hu.arnoldfarkas.flickruploader.flickr.FlickrFolderInfo;
import hu.arnoldfarkas.flickruploader.flickr.FlickrHelper;
import hu.arnoldfarkas.flickruploader.util.Utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import org.scribe.model.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        if (args == null || args.length < 1) {
            findAuthorizationUrl();
            return;
        }

        String baseFolderPath = args[0];
        LOGGER.debug("FileUploader running on {}", baseFolderPath);
        File baseFolder = new File(baseFolderPath);

        FlickrFolderFinder finder = new FlickrFolderFinder(baseFolder);
        for (FlickrFolderInfo folderInfo : finder.find()) {
            folderInfo.update();
        }

        Utils.shutdownExecutor();
    }

    private static void findAuthorizationUrl() {

        System.out.println("args[]{basefolderPath}");
        System.out.println();
        System.out.println("----------------------");
        System.out.println();

        Scanner scanner = new Scanner(System.in);
        FlickrHelper helper = new FlickrHelper();
        System.out.println(helper.getAuthorizationUrl());
        System.out.print(">>");
        String token = scanner.next();
        System.out.println("Token: " + token);
        initProperties(helper.initRequestToken(token));
    }

    private static void initProperties(Token token) {
        try {
            Properties p = new Properties();
            InputStream is = new FileInputStream("flickr.properties");
            p.load(is);
            
            p.setProperty("flickr.requesttoken.token", token.getToken());
            p.setProperty("flickr.requesttoken.secret", token.getSecret());
            
            p.store(new FileOutputStream("flickr.properties"), null);
        } catch (Exception e) {
            LOGGER.error("init properties error", e);
        }
    }
}
