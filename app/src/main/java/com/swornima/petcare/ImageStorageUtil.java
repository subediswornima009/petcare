package com.swornima.petcare;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public final class ImageStorageUtil {

    private static final String IMAGE_DIR = "petcare_images";

    private ImageStorageUtil() {
    }

    public static Uri copyImageToPrivateStorage(Context context, Uri sourceUri) throws IOException {
        File imageDir = new File(context.getFilesDir(), IMAGE_DIR);
        if (!imageDir.exists() && !imageDir.mkdirs()) {
            throw new IOException("Unable to create image storage directory.");
        }

        File destination = new File(imageDir, UUID.randomUUID() + ".jpg");
        try (InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
             FileOutputStream outputStream = new FileOutputStream(destination)) {
            if (inputStream == null) {
                throw new IOException("Unable to open selected image.");
            }

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        return Uri.fromFile(destination);
    }

    public static void deletePrivateImageIfOwned(Context context, String uriString) {
        if (uriString == null || uriString.trim().isEmpty()) {
            return;
        }

        Uri uri = Uri.parse(uriString);
        if (!"file".equals(uri.getScheme())) {
            return;
        }

        try {
            File imageDir = new File(context.getFilesDir(), IMAGE_DIR).getCanonicalFile();
            File imageFile = new File(uri.getPath()).getCanonicalFile();
            if (isInDirectory(imageFile, imageDir)) {
                imageFile.delete();
            }
        } catch (IOException ignored) {
        }
    }

    private static boolean isInDirectory(File file, File directory) throws IOException {
        String filePath = file.getCanonicalPath();
        String directoryPath = directory.getCanonicalPath();
        return filePath.equals(directoryPath) || filePath.startsWith(directoryPath + File.separator);
    }
}
