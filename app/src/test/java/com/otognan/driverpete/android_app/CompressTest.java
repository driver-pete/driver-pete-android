package com.otognan.driverpete.android_app;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.BufferedReader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


public class CompressTest {

    @Test
    public void testBinaryCompression() throws Exception {
        String string = "I am what I am hhhhhhhhhhhhhhhhhhhhhhhhhhhhh"
                + "bjggujhhhhhhhhh"
                + "rggggggggggggggggggggggggg"
                + "esfffffffffffffffffffffffffffffff"
                + "esffffffffffffffffffffffffffffffff"
                + "esfekfgy enter code here`etd`enter code here wdd"
                + "heljwidgutwdbwdq8d"
                + "skdfgysrdsdnjsvfyekbdsgcu"
                +"jbujsbjvugsduddbdj";

        byte[] compressed = Compress.compress(string);
        BufferedReader decompressedReader = Compress.decompress(compressed);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = decompressedReader.readLine()) != null) {
            sb.append(line);
        }
        decompressedReader.close();
        assertThat(string, CoreMatchers.equalTo(sb.toString()));
    }

}
