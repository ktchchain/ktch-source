package com.photon.photonchain.network.utils;

/**
 * @Author:Lin
 * @Description:
 * @Date:15:09 2018/1/31
 * @Modified by:
 */
public class TokenUtil {
    public static double TokensRate(String tokenName) {
        double rate = 1000;
        return rate / tokenName.length();
    }
}
