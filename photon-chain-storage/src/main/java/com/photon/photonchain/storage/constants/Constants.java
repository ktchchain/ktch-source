package com.photon.photonchain.storage.constants;

import java.math.BigInteger;

/**
 * @Author:Lin
 * @Description:
 * @Date:15:13 2018/1/4
 * @Modified by:
 */
public class Constants {
    public static final int MAGIC_NO = 0x133c9c4;
    public static final BigInteger CUMULATIVE_DIFFICULTY = BigInteger.ZERO;
    public static final int BLOCK_VERSION = 1;
    public static final String SYNC_BLOCK_HEIGHT = "SYNC_BLOCK_HEIGHT";
    public static final String SYNC_PARTICIPANT_LIST = "SYNC_PARTICIPANT_LIST";
    public static final String SYNC_BLOCK_LIST = "SYNC_BLOCK_LIST";
    public static final String SYNC_TRANSACTION_LIST = "SYNC_TRANSACTION_LIST";
    public static final String TOTAL_INCOME = "TOTAL_INCOME";
    public static final String TOTAL_EXPENDITURE = "TOTAL_EXPENDITURE";
    public static final String TOTAL_EFFECTIVE_INCOME = "TOTAL_EFFECTIVE_INCOME";
    public static final String BALANCE = "BALANCE";
    public static final String SYNC_MAC_ADDRESS = "SYNC_MAC_ADDRESS";
    public static final int BLOCK_INTERVAL = 0x1f40;
    public static final int OVERTIME_INTERVAL = 0x4e20;
    public static final long MININUMUNIT = 1000000;
    public static final String ADDRESS_PREFIX = "kx";
    public static final String MINI_UNIT = "MINI_UNIT";
    public static final String MAX_UNIT = "MAX_UNIT";
    public static final String PTN = "KTCH";
    public static final int BLOCK_TRANSACTION_SIZE = 0xFF;
    public static final String PUBKEY = "pubKey";
    public static final String PRIKEY = "priKey";
    public static final String PWD = "pwd";
    public static final String PUBKEY_FLAG = " pubKey ";
    public static final String PRIKEY_FLAG = " priKey ";
    public static final String PWD_FLAG = " pwd ";
    public static final String ADDRESS = "address";
    public static final Long MAX_PTN_AMOUNT = 65000000L;
    public static final Long MAX_PTN_AMOUT_UNIT = MAX_PTN_AMOUNT * MININUMUNIT;
    public static final int FORGABLE_NODES = 1;
    public static final int SYNC_SIZE = 0x7d0;
    public static final String CAPITAL_ADDRESS = "04353769aeb7492eaeed79153c05f917d71cd0320dc9ee1d1a70049341cf65ec189c0064507d91774301d43530f045422f32aac787ced24e2650d48ad42f5508de";
    //合约资金流水累计总额临界值
    public static final long CONTRACT_FUNDS_SUM = 100 * MININUMUNIT;
    //合约资金流水总数临界值
    public static final long CONTRACT_FUNDS_COUNT = 1000;
    //流水为5的时候合约第二条流水使用
    public static final String EXCHANGE = "exChange";
    public static final Long SYNC_WAIT_TIME = 0xbb80L;
    public static final int SYNC_WAIT_COUNT = 1;
    public static final int ACTIVE_NODE_COUNT = 1;
    public static final long GENESIS_TIME = 1514736000000L;
    public static String serialNumber[] = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};
    public static boolean IS_SERVER = true;
}
