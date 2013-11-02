package hu.arnoldfarkas.flickruploader.flickr;

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

            
            
            FlickrProperties.save(FlickrProperties.PROP_REQUEST_TOKEN, token.getToken());
            FlickrProperties.save(FlickrProperties.PROP_REQUEST_SECRET, token.getSecret());
 
    }
}
