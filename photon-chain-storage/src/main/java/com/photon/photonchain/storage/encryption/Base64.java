package com.photon.photonchain.storage.encryption;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * @Author:Lin
 * @Description:
 * @Date:17:20 2018/1/8
 * @Modified by:
 */
public class Base64 {
    //解码返回byte
    public static byte[] decryptBASE64(String key) throws Exception {
        return (new BASE64Decoder()).decodeBuffer(key);
    }

    //编码返回字符串
    public static String encryptBASE64(byte[] key) throws Exception {
        return (new BASE64Encoder()).encodeBuffer(key);
    }
}
