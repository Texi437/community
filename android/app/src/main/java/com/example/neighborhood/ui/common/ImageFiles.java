package com.example.neighborhood.ui.common;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import androidx.exifinterface.media.ExifInterface;
import java.io.*;
import java.util.UUID;

public final class ImageFiles {
  private ImageFiles() {}

  public static String importImage(Context context, Uri uri) {
    try {
      BitmapFactory.Options bounds = new BitmapFactory.Options();
      bounds.inJustDecodeBounds = true;
      try (InputStream input = context.getContentResolver().openInputStream(uri)) {
        BitmapFactory.decodeStream(input, null, bounds);
      }
      if (bounds.outWidth < 1) throw new IOException("无效图片");
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inSampleSize = 1;
      while (Math.max(bounds.outWidth, bounds.outHeight) / options.inSampleSize > 1600)
        options.inSampleSize *= 2;
      Bitmap bitmap;
      try (InputStream input = context.getContentResolver().openInputStream(uri)) {
        bitmap = BitmapFactory.decodeStream(input, null, options);
      }
      if (bitmap == null) throw new IOException("无法读取图片");
      int orientation;
      try (InputStream input = context.getContentResolver().openInputStream(uri)) {
        orientation =
            new ExifInterface(input)
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
      }
      Matrix matrix = new Matrix();
      if (orientation == ExifInterface.ORIENTATION_ROTATE_90) matrix.postRotate(90);
      if (orientation == ExifInterface.ORIENTATION_ROTATE_180) matrix.postRotate(180);
      if (orientation == ExifInterface.ORIENTATION_ROTATE_270) matrix.postRotate(270);
      if (orientation == ExifInterface.ORIENTATION_FLIP_HORIZONTAL) matrix.postScale(-1, 1);
      if (orientation == ExifInterface.ORIENTATION_FLIP_VERTICAL) matrix.postScale(1, -1);
      if (orientation == ExifInterface.ORIENTATION_TRANSPOSE) {
        matrix.postRotate(90);
        matrix.postScale(-1, 1);
      }
      if (orientation == ExifInterface.ORIENTATION_TRANSVERSE) {
        matrix.postRotate(270);
        matrix.postScale(-1, 1);
      }
      Bitmap rotated =
          Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      rotated.compress(Bitmap.CompressFormat.JPEG, 85, bytes);
      if (bytes.size() > 2 * 1024 * 1024) throw new IOException("图片压缩后仍超过 2 MB，请选择较小的图片");
      File directory = new File(context.getFilesDir(), "attachments");
      if (!directory.exists() && !directory.mkdirs()) throw new IOException("无法保存图片");
      File file = new File(directory, UUID.randomUUID() + ".jpg");
      try (FileOutputStream output = new FileOutputStream(file)) {
        bytes.writeTo(output);
      }
      if (rotated != bitmap) rotated.recycle();
      bitmap.recycle();
      return file.getAbsolutePath();
    } catch (IOException | SecurityException e) {
      throw new IllegalArgumentException("图片读取失败，请重新选择", e);
    }
  }
}
