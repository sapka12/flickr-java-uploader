package hu.arnoldfarkas.flickruploader.util;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

    private static final SimpleDateFormat monthDateformat = new SimpleDateFormat("yyyy-MM");

    public static String formattedMonth() {
        return formattedMonth(new Date());
    }

    public static String formattedMonth(Date date) {
        return monthDateformat.format(date);
    }

    public static File[] jpgFilesInDirectory(File directory) {
        return directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isFile() && isJpg(f);
            }
        });
    }

    public static boolean isJpg(File file) {
        return file.getAbsolutePath().toLowerCase().endsWith(".jpg");
    }
}
