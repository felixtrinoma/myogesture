package fr.trinoma.myogesture;

import android.content.Context;

import androidx.core.content.ContextCompat;

import java.io.File;

public class FileProvider {

    private final File root;

    public FileProvider(File root) {
        this.root = root;
    }

    public static FileProvider makeOnSdCard(Context context) {
        File[] list = ContextCompat.getExternalFilesDirs(context, null);
        // TODO let the user chose his destination
        File root = list[list.length-1];
        return new FileProvider(root);
    }

    // TODO ensures not already existing
    public File get(String filename) {
        return new File(root, filename);
    }
}
