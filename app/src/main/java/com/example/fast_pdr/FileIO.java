package com.example.fast_pdr;



import android.content.Context;
import android.os.Environment;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.io.IOException;
import android.util.Log;

public class FileIO {

    private Context thisContext;

    public File fileDir;
    public FileIO(Context context) {
        thisContext = context;
    }

    public void setDirTime() {
        fileDir = new File(thisContext.getExternalFilesDir(null), System.currentTimeMillis() + "");
        if (!fileDir.mkdirs()) {
            Log.e("FileIO", "Directory creation failed");
        }
    }

    /**
     * @param fileName example:"111.txt"
     * @return File.class named as "filename"
     */
    public File openFile(String fileName) {

        File file = new File(fileDir, fileName);
        file.setWritable(true);
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    Log.e("FileIO", "File creation failed");
                }
            } catch (IOException e) {
                Log.e("FileIO", "File creation failed", e);
            }
        }
        return file;
    }

    /**
     * @param fileName example:"111.txt"
     * @param content example:"This is com."
     */
    public void writeToFile(String fileName, String content) {
        File file = openFile(fileName);
        Writer writer = null;

        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
            writer.write(content);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param fileName example:"111.txt"
     */
    public void deleteFile(String fileName) {
        File file = new File(fileDir, fileName);
        if (file.exists() && file.isFile()) {
            if (!file.delete()) {
                Log.e("FileIO", "File deletion failed");
            }
        }
    }
}
