package com.czm.xcimageloader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import com.czm.xcimageloader.ImageUtils.ImageSize;
/**
 * Created by caizhiming on 2015/9/10.
 */
public class ImageDownloadUtils {
    /**
     * 根据url下载图片到指定的文件
     *
     * @param urlStr
     * @param file
     * @return
     */
    public static boolean downloadImageByUrl(String urlStr, File file)
    {
        FileOutputStream fos = null;
        InputStream is = null;
        try
        {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            is = conn.getInputStream();
            fos = new FileOutputStream(file);
            byte[] buf = new byte[512];
            int len = 0;
            while ((len = is.read(buf)) != -1)
            {
                fos.write(buf, 0, len);
            }
            fos.flush();
            return true;

        } catch (Exception e)
        {
            e.printStackTrace();
        } finally
        {
            try
            {
                if (is != null)
                    is.close();
                if (fos != null)
                    fos.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 根据url下载图片并压缩
     */
    public static Bitmap downloadImageByUrl(String urlStr, ImageView imageview)
    {
        InputStream is = null;
        try
        {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            is = new BufferedInputStream(conn.getInputStream());
            is.mark(is.available());
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, opts);
            //获取imageview想要显示的宽和高
            ImageSize imageViewSize = ImageUtils.getImageViewSize(imageview);
            opts.inSampleSize = ImageUtils.calculateInSampleSize(opts,
                    imageViewSize.width, imageViewSize.height);
            opts.inJustDecodeBounds = false;
            is.reset();
            bitmap = BitmapFactory.decodeStream(is, null, opts);
            conn.disconnect();
            return bitmap;
        } catch (Exception e)
        {
            e.printStackTrace();
        } finally
        {
            try
            {
                if (is != null)
                    is.close();
            } catch (IOException e)
            {
            }
        }
        return null;
    }
}
