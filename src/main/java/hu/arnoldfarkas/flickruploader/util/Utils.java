package hu.arnoldfarkas.flickruploader.util;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Utils {

    private static final SimpleDateFormat monthDateformat = new SimpleDateFormat("yyyy-MM");
    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(20));

    public static String formattedMonth() {
        return formattedMonth(new Date());
    }

    public static String formattedMonth(Date date) {
        return monthDateformat.format(date);
    }

    public static File[] jpgFilesInDirectory(File directory) {
        return directory.listFiles(new FileFilter() {
            public boolean accept(File f) {
                return f.isFile() && isJpg(f);
            }
        });
    }
    
    public static void addJobToExecutor(Runnable job) {
        executor.execute(job);
    }

    public static void shutdownExecutor() {
        executor.shutdown();
    }
    
    public static  boolean isJpg(File file) {
        return file.getAbsolutePath().toLowerCase().endsWith(".jpg");
    }
}
