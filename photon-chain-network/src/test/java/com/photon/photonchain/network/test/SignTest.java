package com.photon.photonchain.network.test;

import com.alibaba.fastjson.JSON;
import com.photon.photonchain.network.utils.NetWorkUtil;
import com.photon.photonchain.storage.encryption.ECKey;
import com.photon.photonchain.storage.encryption.ECKey.ECDSASignature;
import com.photon.photonchain.storage.encryption.SHAEncrypt;
import java.math.BigInteger;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import javax.sound.midi.Soundbank;

/**
 * @author Wu Created by SKINK on 2018/2/2.
 */

public class SignTest {

  public static void main(String[] args) {
    int a = 0x3e8;
    System.out.println(NetWorkUtil.ipToHexString("47.92.213.211"));
    System.out.println(NetWorkUtil.ipToHexString("47.92.80.208"));
    System.out.println(NetWorkUtil.ipToHexString("47.92.113.128"));
  }
}
