package com.photon.photonchain.network.core;

import com.photon.photonchain.network.ehcacheManager.InitializationManager;
import com.photon.photonchain.storage.repository.BlockRepository;
import com.photon.photonchain.storage.repository.TokenRepository;
import com.photon.photonchain.storage.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * @Author:Lin
 * @Description:
 * @Date:10:25 2018/3/27
 * @Modified by:
 */
@Component
public class ResetData {
    @Autowired
    private BlockRepository blockRepository;
    @Autowired
    private TokenRepository tokenRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private GenesisBlock genesisBlock;
    @Autowired
    private Initialization initialization;
    @Autowired
    private SyncBlock syncBlock;
    @Autowired
    private InitializationManager initializationManager;

    @Transactional
    public void resetAll() {
        tokenRepository.truncate ( );
        blockRepository.truncateRelation ( );
        transactionRepository.truncate ( );
        blockRepository.truncate ( );
        genesisBlock.init ( );
        initialization.init ( );
        syncBlock.init ( );
    }

    @Transactional
    public void resetAssets() {

    }

    @Transactional
    public void backBlock(int blockHeight) {
        long backBlockHeight = initializationManager.getBlockHeight ( ) - blockHeight;
        List<String> signatureList = blockRepository.getSignatureByBlockHeight ( backBlockHeight );
        blockRepository.deleteRelation ( signatureList );
        blockRepository.deleteBlock ( backBlockHeight );
        List<String> tokenNameList = transactionRepository.getTokenNameByQuerey ( backBlockHeight );
        transactionRepository.deleteByBlockHeight ( backBlockHeight );
        tokenRepository.deleteByTokenName ( tokenNameList );
        initializationManager.setLastBlock ( blockRepository.findBlockByBlockId ( backBlockHeight ) );
    }

    @Transactional
    public void backBlockByblockHeight(long blockHeight) {
        List<String> signatureList = blockRepository.getSignatureByBlockHeight ( blockHeight );
        blockRepository.deleteRelation ( signatureList );
        blockRepository.deleteBlock ( blockHeight );
        List<String> tokenNameList = transactionRepository.getTokenNameByQuerey ( blockHeight );
        transactionRepository.deleteByBlockHeight ( blockHeight );
        tokenRepository.deleteByTokenName ( tokenNameList );
        initializationManager.setLastBlock ( blockRepository.findBlockByBlockId ( blockHeight ) );
    }
}
