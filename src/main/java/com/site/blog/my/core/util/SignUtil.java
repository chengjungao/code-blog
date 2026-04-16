package com.site.blog.my.core.util;

import java.security.MessageDigest;
import java.util.Arrays;

public class SignUtil {

    public static boolean checkSignature(String signature, String timestamp, String nonce,String TOKEN) {
        String[] arr = new String[]{TOKEN, timestamp, nonce};
        Arrays.sort(arr);
        StringBuilder content = new StringBuilder();
        for (String s : arr) {
            content.append(s);
        }
        return sha1(content.toString()).equals(signature);
    }

    private static String sha1(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest(str.getBytes());
            StringBuilder hexStr = new StringBuilder();
            for (byte b : bytes) {
                hexStr.append(String.format("%02x", b));
            }
            return hexStr.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
