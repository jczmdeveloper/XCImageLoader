package com.czm.xcimageloader;

import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;

/**
 * Created by caizhiming on 2015/9/10.
 */
public class ImageUtils {
    /**
     * 根据ImageView的宽高和图片实际的宽高计算SampleSize
     */
    public static int calculateInSampleSize(BitmapFactory.Options options,
          int reqWidth,int reqHeight)
    {
        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;
        if (width > reqWidth || height > reqHeight)
        {
            int widthRadio = Math.round(width * 1.0f / reqWidth);
            int heightRadio = Math.round(height * 1.0f / reqHeight);
            inSampleSize = Math.max(widthRadio, heightRadio);
        }
        return inSampleSize;
    }

    /**
     * 获取ImageView所要显示的宽和高
     */
    public static ImageSize getImageViewSize(ImageView imageView)
    {
        ImageSize imageSize = new ImageSize();
        DisplayMetrics displayMetrics = imageView.getContext().getResources()
                .getDisplayMetrics();
        ViewGroup.LayoutParams lp = imageView.getLayoutParams();
        // 获取imageview的实际宽度
        int width = imageView.getWidth();
        if (width <= 0)
        {// 获取imageview在layout中声明的宽度
            width = lp.width;
        }
        if (width <= 0)
        {// 检查最大值
            width = getImageViewFieldValue(imageView, "mMaxWidth");
        }
        if (width <= 0)
        {
            width = displayMetrics.widthPixels;
        }
        // 获取imageview的实际高度
        int height = imageView.getHeight();
        if (height <= 0)
        {// 获取imageview在layout中声明的宽度
            height = lp.height;
        }
        if (height <= 0)
        {// 检查最大值
            height = getImageViewFieldValue(imageView, "mMaxHeight");
        }
        if (height <= 0)
        {
            height = displayMetrics.heightPixels;
        }
        imageSize.width = width;
        imageSize.height = height;
        return imageSize;
    }
    public static class ImageSize
    {
        int width;
        int height;
    }
    /**
     * 通过反射机制获取imageview的某个属性值
     */
    private static int getImageViewFieldValue(Object object, String fieldName)
    {
        int value = 0;
        try
        {
            Field field = ImageView.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            int fieldValue = field.getInt(object);
            if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE)
            {
                value = fieldValue;
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return value;
    }
}
