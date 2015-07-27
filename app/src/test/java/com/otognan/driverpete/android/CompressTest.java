package com.otognan.driverpete.android;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
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
        String decompressed = Compress.decompress(compressed);
        assertThat(string, CoreMatchers.equalTo(decompressed));
    }

}
