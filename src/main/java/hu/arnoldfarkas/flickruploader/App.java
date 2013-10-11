package hu.arnoldfarkas.flickruploader;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        LOGGER.debug("input: {}", Arrays.asList(args));
        TaskChooser taskChooser = new TaskChooser();
        taskChooser.execute(args);
    }
}
