package hu.arnoldfarkas.flickruploader.flickr;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;
import org.scribe.model.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlickrRequestTokenFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlickrRequestTokenFinder.class);

    public FlickrRequestTokenFinder() {
    }

    public Token find() {
        Scanner scanner = new Scanner(System.in);
        FlickrHelper helper = new FlickrHelper();
        System.out.println(helper.getAuthorizationUrl());
        System.out.print(">>");
        String token = scanner.next();
        System.out.println("Token: " + token);
        Token requestToken = helper.initRequestToken(token);
        initProperties(requestToken);
        return requestToken;
    }

    private void initProperties(Token token) {
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
