package com.photon.photonchain.network.core;


import com.photon.photonchain.storage.constants.Constants;
import com.photon.photonchain.storage.encryption.ECKey;
import com.photon.photonchain.storage.encryption.SHAEncrypt;
import com.photon.photonchain.storage.entity.*;
import com.photon.photonchain.storage.repository.BlockRepository;
import com.photon.photonchain.storage.repository.NodeAddressRepository;
import com.photon.photonchain.storage.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author:Lin
 * @Description:
 * @Date:16:09 2018/1/12
 * @Modified by:
 */
@Component
public class GenesisBlock {
    final static Logger logger = LoggerFactory.getLogger(GenesisBlock.class);


    @Autowired
    private BlockRepository blockRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private NodeAddressRepository nodeAddressRepository;

    public static final long GENESIS_TIME = Constants.GENESIS_TIME;
    public static final long RECEIVE_QUANTITY = 55000000L * Constants.MININUMUNIT;
    private static final byte[] GENESIS_PUBLIC_KEY = {4, -117, 36, -75, -40, 108, -52, 0, -52, -121, -105, -99, 17, 12, -40, 56, 14, -93, -49, 60, 80, 106, 114, -24, 125, -13, -111, 17, 46, 88, -73, -69, -114, -112, -73, -30, 75, 97, 71, -79, 43, 66, -104, -35, -113, -46, 32, 97, -76, 38, -96, 33, 123, 59, 48, -16, 109, -36, -71, 60, 46, 47, 44, -105, -3};
    private static final byte[] ACCEPTER = {4, -42, 114, 56, -78, -53, -117, 78, 83, -4, -122, 122, 33, 104, -118, -53, 72, -23, -82, 51, 109, -49, 109, -46, -21, -33, 70, 87, 20, 90, 46, 39, -61, -47, -5, -128, -14, -65, 109, 77, 20, -69, 72, 42, -34, 116, 121, -16, 89, 52, -18, -94, -103, -110, -104, 16, 32, -116, -119, 0, 64, 41, -127, -16, 127};
    private static final String HASH_PREV_BLOCK = "aced000570";
    private static final String TRANS_SIGNATURE = "7b2272223a3131353036333837373932353536313531313535323839303939303136383731393937333437333232333936373537323134363833343636313931323736303237393734393035313434373437332c2273223a33343333363531373533383830303833303437303337373236363830393337313939323838343431393833353034323238383636303037373333353438333830383930393438353739333032332c2276223a32377d";
    private static final String HASH_MERKLE_ROOT = "66643532373234316234333536343934323265663062383136366233323137623836636538343532323431356262616538643262643637326562383439653937";
    private static final String BLOCK_SIGNATURE = "7b2272223a37383532343539353931373534313335333537383131373134343036343130363532333932373535363433333536343832353137313038373336353339343731343737313834393335383130302c2273223a33303435343936313535383830323032393035333638383434333634343638313338303339363836333931343230353633373431333436363634343439343636373535383638363735303633392c2276223a32377d";

    @Transactional(rollbackFor = Exception.class)
    public void init() {
        if (blockRepository.count() == 0) {
            logger.info("MainAccount:" + ECKey.pubkeyToAddress(Hex.toHexString(ACCEPTER)));
            TransactionHead transactionHead = new TransactionHead(Hex.toHexString(GENESIS_PUBLIC_KEY), Hex.toHexString(ACCEPTER), RECEIVE_QUANTITY, 0, GENESIS_TIME);
            BlockHead blockHead = new BlockHead(Constants.BLOCK_VERSION, GENESIS_TIME, Constants.CUMULATIVE_DIFFICULTY, Hex.decode(HASH_PREV_BLOCK), Hex.decode(HASH_MERKLE_ROOT));
            Transaction transaction = new Transaction(Hex.decode(TRANS_SIGNATURE), transactionHead, 0, 0, Hex.toHexString(GENESIS_PUBLIC_KEY), Hex.toHexString(ACCEPTER), "", Constants.PTN, 1, RECEIVE_QUANTITY, 0);
            List<Transaction> transactionList = new ArrayList<>();
            transactionList.add(transaction);
            Block block = new Block(0, 441, RECEIVE_QUANTITY, 0, Hex.decode(BLOCK_SIGNATURE), GENESIS_PUBLIC_KEY, blockHead, transactionList);
            block.setBlockHash(Hex.toHexString(SHAEncrypt.SHA256(blockHead)));
            System.out.println(block.getBlockHash());
            transactionRepository.save(transaction);
            blockRepository.save(block);
        }
    }
}
