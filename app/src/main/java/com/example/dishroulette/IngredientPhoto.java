package com.example.dishroulette;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;

import androidx.exifinterface.media.ExifInterface;

import java.io.*;

/**
 * Samples before decoding, applies camera orientation and strips metadata by re-encoding.
 */
final class IngredientPhoto {
    static File prepare(Context context, Uri source) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream in = context.getContentResolver().openInputStream(source)) {
            BitmapFactory.decodeStream(in, null, options);
        }
        if (options.outWidth <= 0 || options.outHeight <= 0 || options.outWidth > 100_000 ||
            options.outHeight > 100_000)
            throw new IOException("Unsupported image");
        int sample = 1;
        while (Math.max(options.outWidth, options.outHeight) / sample > 1600)
            sample *= 2;
        options.inSampleSize = sample;
        options.inJustDecodeBounds = false;
        Bitmap bitmap;
        try (InputStream in = context.getContentResolver().openInputStream(source)) {
            bitmap = BitmapFactory.decodeStream(in, null, options);
        }
        if (bitmap == null)
            throw new IOException("Unsupported image");
        File output = null;
        try {
            Matrix matrix = new Matrix();
            try (InputStream in = context.getContentResolver().openInputStream(source)) {
                if (in != null) {
                    ExifInterface exif = new ExifInterface(in);
                    // ExifInterface defines rotation after the horizontal flip.
                    if (exif.isFlipped())
                        matrix.postScale(-1, 1);
                    matrix.postRotate(exif.getRotationDegrees());
                }
            } catch (IOException ignored) { /* Images without EXIF are valid. */ }
            if (!matrix.isIdentity()) {
                Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                if (rotated != bitmap) {
                    bitmap.recycle();
                    bitmap = rotated;
                }
            }
            File directory = new File(context.getCacheDir(), "ingredient-photos");
            if (!directory.isDirectory() && !directory.mkdirs())
                throw new IOException();
            output = File.createTempFile("scan-", ".jpg", directory);
            try (OutputStream stream = new FileOutputStream(output)) {
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream))
                    throw new IOException();
            }
            if (output.length() > 3_000_000)
                throw new IOException("Image too large");
            return output;
        } catch (IOException | RuntimeException e) {
            if (output != null)
                output.delete();
            throw e;
        } finally {
            bitmap.recycle();
        }
    }
}
