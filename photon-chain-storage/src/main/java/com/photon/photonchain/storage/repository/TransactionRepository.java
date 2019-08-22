package com.photon.photonchain.storage.repository;


import com.photon.photonchain.storage.entity.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * @Author:Lin
 * @Description:
 * @Date:15:49 2017/12/29
 * @Modified by:
 */
public interface TransactionRepository extends CrudRepository<Transaction, Long> {
    @Query(value = "select transaction from Transaction transaction where transaction.blockHeight = -1")
    List<Transaction> unconfirmedTransaction(Pageable pageable);

    @Modifying
    @Query(value = "update Transaction trans set trans.blockHeight=:#{#transaction.blockHeight},trans.lockTime=:#{#transaction.lockTime}")
    void updateTransaction(@Param("transaction") Transaction transaction);

    @Query(value = "select transaction from Transaction transaction where transaction.transFrom=:#{#Account} or transaction.transTo=:#{#Account}")
    List<Transaction> findAllByAccount(@Param("Account") String Account);

    @Query(value = "select transaction from Transaction transaction where transaction.transTo=:#{#Account}")
    List<Transaction> findAllByTransTo(@Param("Account") String Account);

    @Query(value = "select transaction from Transaction transaction")
    List<Transaction> findByTransactionId(@Param("transactionId") long transactionId);

    @Query(value = "select transaction from Transaction transaction where transaction.transSignature in (:#{#signatureList})")
    List<Transaction> findByTransSignatureList(@Param("signatureList") List<byte[]> signatureList);

    @Query(value = "select transaction from Transaction transaction")
    List<Transaction> findTransactionOne(Pageable pageable);


    @Query(value = "select transaction from Transaction transaction where transaction.transFrom=:#{#Account} or transaction.transTo=:#{#Account}")
    List<Transaction> findAllByAccount(@Param("Account") String Account, Pageable pageable);

    @Query(value = "select transaction from Transaction transaction where transaction.transSignature=:#{#transSignature}")
    List<Transaction> findAllByTransSignature(@Param("transSignature") byte[] transSignature);

    @Query(value = "select transaction from Transaction transaction where lower(transaction.tokenName)=lower(:#{#tokenName}) and transaction.transType=1")
    Transaction findByTokenNameAndTransType(@Param("tokenName") String tokenName);

    @Query(value = "select transaction from Transaction transaction where lower(transaction.tokenName)=lower(:#{#tokenName})")
    List<Transaction> findTransaction(@Param("tokenName") String tokenName, Pageable pageable);

    @Query(value = "select transaction from Transaction transaction where lower(transaction.tokenName)=lower(:#{#tokenName}) and transaction.transType=:#{#transType} and (transaction.transFrom=:#{#account} or transaction.transTo=:#{#account})")
    List<Transaction> findAllByAccountAndTokenName(@Param("tokenName") String tokenName, @Param("transType") int transType, @Param("account") String account, Pageable pageable);

    @Query(value = "select transaction from Transaction transaction where (transaction.transFrom=:#{#Account} or transaction.transTo=:#{#Account}) AND (lower(transaction.tokenName)=lower(:#{#tokenName}))")
    List<Transaction> findAllByAccountAndTokenName(@Param("Account") String Account, @Param("tokenName") String tokenName, Pageable pageable);

    @Query(value = "select transaction from Transaction transaction ")
    List<Transaction> findAll();

    @Query(value = "select transaction from Transaction transaction order by transaction.blockHeight asc")
    List<Transaction> findAllASC();

    @Query(value = "select * from Transaction order by BLOCK_HEIGHT ASC limit 1", nativeQuery = true)
    Transaction findAllASCLimitOne();

    @Query(value = "select * from Transaction order by BLOCK_HEIGHT DESC limit 1", nativeQuery = true)
    Transaction findAllDESCLimitOne();

    @Query(value = "select trans from Transaction trans where trans.blockHeight>:#{#blockHeight} order by trans.blockHeight asc")
    List<Transaction> findAllByGtBlockHeight(@Param("blockHeight") long blockHeight);


    @Query(value = "select trans from Transaction trans where trans.blockHeight>:#{#start} and trans.blockHeight<=:#{#end} order by trans.blockHeight asc")
    List<Transaction> findAllByBlockHeight(@Param("start") long start, @Param("end") long end);

    @Modifying
    @Query(value = "insert into TRANSACTION(TRANS_SIGNATURE,BLOCK_HEIGHT,LOCK_TIME,REMARK,TOKEN_NAME,TRANS_FROM,TRANS_TO,TRANS_TYPE,TRANSACTION_HEAD, CONTRACT_ADDRESS ,CONTRACT_BIN ,CONTRACT_STATE ,CONTRACT_TYPE ,EXCHENGE_TOKEN, TRANS_VALUE ,FEE) values(:#{#transaction.transSignature},:#{#transaction.blockHeight},:#{#transaction.lockTime},:#{#transaction.remark},:#{#transaction.tokenName},:#{#transaction.transFrom},:#{#transaction.transTo},:#{#transaction.transType},:#{#transaction.transactionHead},:#{#transaction.contractAddress},:#{#transaction.contractBin},:#{#transaction.contractState},:#{#transaction.contractType},:#{#transaction.exchengeToken},:#{#transaction.transValue},:#{#transaction.fee})", nativeQuery = true)
    void saveTransaction(@Param("transaction") Transaction transaction) throws Exception;

    @Query(value = "select count(transaction.transSignature) from Transaction transaction where lower(transaction.tokenName)=lower(:#{#tokenName})")
    int findTransactionCount(@Param("tokenName") String tokenName);

    @Query(value = "select count(transaction.transSignature) from Transaction transaction where transaction.transFrom=:#{#account} AND lower(transaction.tokenName)=lower(:#{#tokenName})")
    int findTransByTransFromCount(@Param("account") String account, @Param("tokenName") String tokenName);

    @Query(value = "select count(transaction.transSignature) from Transaction transaction where transaction.transTo=:#{#account} AND lower(transaction.tokenName)=lower(:#{#tokenName})")
    int findTransByTransToCount(@Param("account") String account, @Param("tokenName") String tokenName);

    @Query(value = "select * from Transaction where lower(token_name) =lower(:#{#tokenName}) AND trans_from=:#{#account} limit :#{#start},:#{#end}", nativeQuery = true)
    List<Transaction> findTransByTransFrom(@Param("account") String account, @Param("tokenName") String tokenName, @Param("start") long start, @Param("end") long end);

    @Query(value = "select * from Transaction where lower(token_name) =lower(:#{#tokenName}) AND trans_to=:#{#account} limit :#{#start},:#{#end}", nativeQuery = true)
    List<Transaction> findTransByTransTo(@Param("account") String account, @Param("tokenName") String tokenName, @Param("start") long start, @Param("end") long end);

    @Query(value = "select count(transaction.transSignature) from Transaction transaction where lower(transaction.tokenName)=lower(:#{#tokenName}) and (transaction.transFrom=:#{#account} or transaction.transTo=:#{#account})")
    int findAllByAccountAndTokenNameAndCount(@Param("tokenName") String tokenName, @Param("account") String account);

    @Query(value = "select count(transaction.blockHeight) from Transaction transaction where transaction.blockHeight =:#{#blockHeight}")
    int findBlockTransCount(long blockHeight);

    @Query(value = "select * from Transaction limit :#{#start},:#{#size}", nativeQuery = true)
    List<Transaction> findTransactionInterval(@Param("start") long start, @Param("size") long size);

    @Modifying
    @Query(value = "DELETE FROM TRANSACTION ", nativeQuery = true)
    void truncate();

    @Query(value = "select transaction from Transaction transaction where transaction.transSignature=:#{#signature}")
    Transaction findByTransSignature(@Param("signature") byte[] signature);


    /**
     * 根据合同地址查流水
     */
    @Query(value = "select transaction from Transaction transaction where transaction.contractAddress=:#{#contractAddress} and transaction.transType=:#{#transType}")
    Transaction findByContract(@Param("contractAddress") String contractAddress, @Param("transType") int transType);

    /**
     * 批量根据合同地址查流水
     */
    @Query(value = "select transaction from Transaction transaction where transaction.transType=:#{#transType} and transaction.contractAddress in (:#{#contracts})")
    List<Transaction> findByContracts(@Param("contracts") List<String> contracts, @Param("transType") int transType);

    /**
     * 根据合同地址和状态查流水
     */
    @Query(value = "select transaction from Transaction transaction where transaction.contractAddress=:#{#contractAddress} and transaction.contractState=:#{#state} and transaction.transType=:#{#transType}")
    Transaction findByContractAddress(@Param("contractAddress") String contractAddress, @Param("state") Integer state, @Param("transType") Integer transType);

    /**
     * 根据合同地址和类型查流水
     */
    @Query(value = "select transaction from Transaction transaction where transaction.transTo=:#{#transTo} and transaction.transType=:#{#transType} and transaction.contractAddress=:#{#contractAddress}")
    Transaction findByTransFromAndType(@Param("transTo") String transTo, @Param("transType") Integer transType, @Param("contractAddress") String contractAddress);

    @Query(value = "SELECT * FROM TRANSACTION  where TRANS_TYPE =:#{#transType} and TRANS_TO =:#{#transTo}", nativeQuery = true)
    List<Transaction> findByTransTypeAndTransToNative(@Param("transType") long transType, @Param("transTo") String transTo);


    @Modifying
    @Query(value = "update Transaction trans set trans.contractState=:#{#transaction.contractState} where trans.contractAddress =:#{#transaction.contractAddress} and trans.transType=:#{#transaction.transType}")
    void updateTransactionState(@Param("transaction") Transaction transaction);

    @Modifying
    @Query(value = "update Transaction trans set trans.contractState=:#{#transaction.contractState} where trans.contractAddress =:#{#transaction.contractAddress}")
    void updateContranctState(@Param("transaction") Transaction transaction);


    @Query(value = "SELECT * FROM TRANSACTION where  CONTRACT_TYPE =:#{#contractType} AND TRANS_TYPE  =:#{#transType} and TRANS_FROM in(:#{#transForms}) order by  BLOCK_HEIGHT, TRANS_SIGNATURE limit :#{#start},:#{#size}", nativeQuery = true)
    List<Transaction> findContractTrans(@Param("contractType") Integer contractType, @Param("transType") Integer transType, @Param("transForms") List<String> transForms, @Param("start") int start, @Param("size") int size);

    @Query(value = "select count(trans.transSignature) from Transaction trans where trans.contractType=:#{#contractType} and trans.transType=:#{#transType} and trans.transFrom in (:#{#transForms})")
    int findTransCount(@Param("contractType") Integer contractType, @Param("transType") Integer transType, @Param("transForms") List<String> transForms);


    @Query(value = "SELECT * FROM TRANSACTION where  CONTRACT_TYPE =:#{#contractType} and TRANS_TYPE =:#{#transType} and TRANS_FROM not in(:#{#transForms}) and CONTRACT_STATE =:#{#contractState} and lower(TOKEN_NAME ) like %:#{#tokenName}% and lower(EXCHENGE_TOKEN ) like %:#{#exchengeToken}% limit :#{#start},:#{#size}", nativeQuery = true)
    List<Transaction> findContractTrans(@Param("contractType") Integer contractType, @Param("transType") Integer transType, @Param("transForms") List<String> transForms, @Param("contractState") int contractState, @Param("tokenName") String tokenName, @Param("exchengeToken") String exchengeToken, @Param("start") int start, @Param("size") int size);


    @Query(value = "SELECT count(*) FROM TRANSACTION where  CONTRACT_TYPE =:#{#contractType} and TRANS_TYPE =:#{#transType} and TRANS_FROM not in(:#{#transForms}) and CONTRACT_STATE =:#{#contractState} and lower(TOKEN_NAME ) like %:#{#tokenName}% and lower(EXCHENGE_TOKEN ) like %:#{#exchengeToken}%", nativeQuery = true)
    int findContractTransCount(@Param("contractType") Integer contractType, @Param("transType") Integer transType, @Param("transForms") List<String> transForms, @Param("contractState") int contractState, @Param("tokenName") String tokenName, @Param("exchengeToken") String exchengeToken);


    @Query(value = "SELECT count(TRANS_SIGNATURE) FROM TRANSACTION  where TRANS_TYPE =:#{#transType} and TRANS_FROM  =:#{#transFrom} and CONTRACT_ADDRESS  =:#{#contractAddress}", nativeQuery = true)
    long findByTransTypeAndTransFromAndContractAddressNative(@Param("transType") long transType, @Param("transFrom") String transFrom, @Param("contractAddress") String contractAddress);

    @Query(value = "SELECT ifnull(sum(trans_value)-sum(fee),0) FROM TRANSACTION where trans_type=2", nativeQuery = true)
    long totalMining();

    @Query(value = "SELECT IFNULL(sum(FEE),0)  FROM TRANSACTION where block_height >:#{#blockHeight} and TRANS_FROM =:#{#transFrom} ", nativeQuery = true)
    long findSumFee(@Param("blockHeight") Long blockHeight,@Param("transFrom") String transFrom);

    @Query(value = "SELECT IFNULL(sum(TRANS_VALUE ),0)  FROM TRANSACTION where block_height >:#{#blockHeight} and TRANS_FROM =:#{#transFrom} and lower(TOKEN_NAME) =lower(:#{#tokenName}) AND TRANS_TYPE !=1", nativeQuery = true)
    long findExpenditureValue(@Param("blockHeight") Long blockHeight,@Param("transFrom") String transFrom, @Param("tokenName") String tokenName);

    @Query(value = "SELECT IFNULL(sum(TRANS_VALUE),0)  FROM TRANSACTION where block_height >:#{#blockHeight} and TRANS_TO =:#{#transTo} and lower(TOKEN_NAME) =lower(:#{#tokenName}) ", nativeQuery = true)
    long findIncome( @Param("blockHeight") Long blockHeight,@Param("transTo") String transTo, @Param("tokenName") String tokenName);

    @Query(value = "select trans.tokenName from Transaction trans where trans.blockHeight>:#{#blockHeight} and trans.transType=1")
    List<String> getTokenNameByQuerey(@Param("blockHeight") long blockHeight);

    @Modifying
    @Query(value = "delete from Transaction trans where trans.blockHeight>:#{#blockHeight}")
    void deleteByBlockHeight(@Param("blockHeight") long blockHeight);

    @Query(value = "select count(transaction.transSignature) from Transaction transaction where transaction.contractAddress=:#{#contractAddress} and transaction.transType=:#{#transType} and transaction.contractType=:#{#contractType}")
    Integer findVodesCount(@Param("contractAddress") String contractAddress, @Param("transType") int transType, @Param("contractType") int contractType);

    @Query(value = "select count(transaction.transSignature) from Transaction transaction where transaction.contractAddress=:#{#contractAddress} and transaction.transType=:#{#transType} and transaction.contractType=:#{#contractType} and transaction.remark =:#{#item}")
    Integer findVodesCount(@Param("contractAddress") String contractAddress, @Param("transType") int transType, @Param("contractType") int contractType, @Param("item") String item);

    @Query(value = "select count(transaction.transSignature) from Transaction transaction where transaction.contractAddress=:#{#contractAddress} and transaction.transFrom =:#{#transFrom} and transaction.transType=:#{#transType} and transaction.contractType=:#{#contractType} and transaction.remark =:#{#item}")
    Integer findVode(@Param("contractAddress") String contractAddress, @Param("transFrom") String transFrom, @Param("transType") int transType, @Param("contractType") int contractType, @Param("item") String item);


    @Query(value = "SELECT * FROM TRANSACTION where CONTRACT_ADDRESS =:#{#contractAddress} and TRANS_TYPE =:#{#transType} and CONTRACT_TYPE =:#{#contractType} and REMARK =:#{#item} limit :#{#start},:#{#size}", nativeQuery = true)
    List<Transaction> findVodes(@Param("contractAddress") String contractAddress, @Param("transType") int transType, @Param("contractType") int contractType, @Param("item") String item, @Param("start") int start, @Param("size") int size);

    @Query(value = "select count(transaction.transSignature) from Transaction transaction where transaction.transType=:#{#transType} and transaction.contractType=:#{#contractType} and transaction.contractState =:#{#contractState}")
    Integer findContractCount(@Param("transType") int transType, @Param("contractType") int contractType, @Param("contractState") int contractState);


    @Query(value = "SELECT * FROM TRANSACTION where TRANS_TYPE =:#{#transType} and  CONTRACT_TYPE  =:#{#contractType} and CONTRACT_STATE =:#{#contractState} limit :#{#start},:#{#size}", nativeQuery = true)
    List<Transaction> findContract(@Param("transType") int transType, @Param("contractType") int contractType, @Param("contractState") int contractState, @Param("start") int start, @Param("size") int size);

    @Query(value = "select transaction from Transaction transaction where transaction.contractAddress=:#{#contractAddress} and transaction.transType=:#{#transType} and transaction.contractType=:#{#contractType}")
    Transaction findContract(@Param("contractAddress") String contractAddress, @Param("transType") int transType, @Param("contractType") int contractType);

    @Query(value = "select count(transaction.transSignature) from Transaction transaction where transaction.contractAddress=:#{#contractAddress} and transaction.transFrom =:#{#transFrom} and transaction.transType=:#{#transType}")
    Integer findVoteContract(@Param("contractAddress") String contractAddress, @Param("transFrom") String transFrom, @Param("transType") Integer transType);

    @Query(value = "SELECT transaction FROM Transaction transaction where lower(transaction.tokenName)=lower(:#{#tokenName}) and transaction.contractType=:#{#contractType}")
    List<Transaction> findTransactionByContract(@Param("tokenName") String tokenName, @Param("contractType") int contractType);

    @Query(value = "select count(transaction.transSignature) from Transaction transaction where lower(transaction.tokenName)=lower(:#{#tokenName})")
    Integer findTransByTokenName(@Param("tokenName") String tokenName);

    @Query(value = "select count(transaction.transSignature) from Transaction transaction where lower(transaction.tokenName)=lower(:#{#tokenName}) and transaction.transType=:#{#transType} ")
    Integer findTransByTokenNameAndType(@Param("tokenName") String tokenName, @Param("transType") int transType);

    @Query(value = "select count(transaction.transSignature) from Transaction transaction where transaction.contractAddress=:#{#contractAddress} and transaction.transFrom=:#{#transFrom} and transaction.transType=:#{#transType} and transaction.contractType=:#{#contractType} and transaction.remark =:#{#item}")
    Integer findVodes(@Param("contractAddress") String contractAddress, @Param("transFrom") String transFrom, @Param("transType") int transType, @Param("contractType") int contractType, @Param("item") String item);


}
