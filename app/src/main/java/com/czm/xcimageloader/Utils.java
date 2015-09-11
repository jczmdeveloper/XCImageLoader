package com.czm.xcimageloader;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by caizhiming on 2015/9/11.
 */
public class Utils {
    /**
     * 利用签名辅助类，将字符串字节数组
     */
    public static String makeMd5(String str)
    {
        byte[] digest = null;
        try
        {
            MessageDigest md = MessageDigest.getInstance("md5");
            digest = md.digest(str.getBytes());
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        return null;
    }
    /**
     *bytes to hex
     */
    public static String bytesToHex(byte[] bytes)
    {
        StringBuilder sb = new StringBuilder();
        String tmp = null;
        for (byte b : bytes)
        {
            // 将每个字节与0xFF进行与运算，然后转化为10进制，然后借助于Integer再转化为16进制
            tmp = Integer.toHexString(0xFF & b);
            if (tmp.length() == 1)
            {// 每个字节8为，转为16进制标志，2个16进制位
                tmp = "0" + tmp;
            }
            sb.append(tmp);
        }
        return sb.toString();

    }
}
