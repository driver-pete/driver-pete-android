package com.otognan.driverpete.android;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Compress {

    public static byte[] compress(String str) throws Exception {
        if (str == null || str.length() == 0) {
            return null;
        }
        ByteArrayOutputStream obj=new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(obj);
        gzip.write(str.getBytes("UTF-8"));
        gzip.close();
        return obj.toByteArray();
    }

    public static String decompress(byte[] inputBytes) throws Exception {
        if (inputBytes == null) {
            return null;
        }
        GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(inputBytes));
        BufferedReader bf = new BufferedReader(new InputStreamReader(gis, "UTF-8"));
        String outStr = "";
        String line;
        while ((line=bf.readLine())!=null) {
            outStr += line;
        }
        return outStr;
    }

}
