package com.photon.photonchain.network.ehcacheManager;

import com.photon.photonchain.storage.constants.Constants;
import com.photon.photonchain.storage.entity.UnconfirmedTran;
import com.photon.photonchain.storage.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: lqh
 * @description: 资产
 * @program: photon-chain
 * @create: 2018-05-23 10:07
 **/
@Component
public class AssetsManager {

    final static Logger logger = LoggerFactory.getLogger ( AssetsManager.class );


    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    UnconfirmedTranManager unconfirmedTranManager;

    @Autowired
    StatisticalAssetsManager statisticalAssetsManager;



    public Map<String, Long> getAccountAssets(String pubKey, String tokenName) {
        Map<String, Map<String, Long>> accountAssets = statisticalAssetsManager.getStatisticalAssets ( );
        Long blockHeight = -1L;
        try {
            blockHeight = accountAssets.get ( Constants.PTN ).get ( Constants.SYNC_BLOCK_HEIGHT );
        } catch (Exception e) {

        }
        long start = System.currentTimeMillis ( );
        Map<String, Long> resultMap = new HashMap<> ( );
        //TODO 计算资产
        long totalIncome = 0;
        long totalExpenditure = 0;
        long totalEffectiveIncome = 0;
        long balance = 0;
        long transExpenditure = 0;
        long untransExpenditure = 0;
        long untransIncome = 0;
        long transIncome = 0;

        try {
            //TODO:unconfirm
            //unConfirm
            long t1 = System.currentTimeMillis ( );
            Map<String, String> unConfirmTransAssets = this.getAccountsUnTransValue ( unconfirmedTranManager.getUnconfirmedTranMap ( ) );
            String unConfirmTransAsset = unConfirmTransAssets.get ( pubKey + "_" + tokenName.toLowerCase ( ) );
            if ( unConfirmTransAsset != null ) {
                untransIncome = Long.parseLong ( unConfirmTransAsset.split ( "," )[0] );
                untransExpenditure = Long.parseLong ( unConfirmTransAsset.split ( "," )[1] );
            }
            long t2 = System.currentTimeMillis ( );

            //confirm
            long t3 = System.currentTimeMillis ( );

            transIncome = transactionRepository.findIncome ( blockHeight, pubKey, tokenName );
            if ( tokenName.equalsIgnoreCase ( Constants.PTN ) ) {
                transExpenditure = transactionRepository.findExpenditureValue ( blockHeight, pubKey, tokenName ) + transactionRepository.findSumFee ( blockHeight, pubKey );
            } else {
                transExpenditure = transactionRepository.findExpenditureValue ( blockHeight, pubKey, tokenName );
            }
            long t4 = System.currentTimeMillis ( );
            totalIncome = transIncome + untransIncome;
            totalExpenditure = transExpenditure + untransExpenditure;
            totalEffectiveIncome = transIncome;
            balance = (totalEffectiveIncome - totalExpenditure) < 0 ? 0L : (totalEffectiveIncome - totalExpenditure);
            long end = System.currentTimeMillis ( );
            logger.info ( "【$$---计算资产用时：{},unconfirm:{},confirm:{}】", (end - start), (t2 - t1), (t4 - t3) );
        } catch (Exception e) {
            logger.error ( "计算资产异常：" + e.getMessage ( ) );
        }
        Map<String, Long> account = accountAssets.get ( pubKey + "_" + tokenName );
        if ( account == null ) {
            account = new HashMap<> ( );
            account.put ( Constants.TOTAL_INCOME, 0L );
            account.put ( Constants.TOTAL_EFFECTIVE_INCOME, 0L );
            account.put ( Constants.TOTAL_EXPENDITURE, 0L );
            account.put ( Constants.BALANCE, 0L );
        }
        resultMap.put ( Constants.TOTAL_INCOME, account.get ( Constants.TOTAL_INCOME ) + totalIncome );
        resultMap.put ( Constants.TOTAL_EFFECTIVE_INCOME, account.get ( Constants.TOTAL_EFFECTIVE_INCOME ) + totalEffectiveIncome );
        resultMap.put ( Constants.TOTAL_EXPENDITURE, account.get ( Constants.TOTAL_EXPENDITURE ) + totalExpenditure );
        resultMap.put ( Constants.BALANCE, account.get ( Constants.BALANCE ) + balance );
        return resultMap;
    }

    /**
     * 计算收入支出
     */
    private static Map<String, String> getAccountsUnTransValue(Map<String, UnconfirmedTran> Unmap) {
        Map<String, String> map = new HashMap ( );//key:XXX,value:收入-支出
        for (String key : Unmap.keySet ( )) {
            UnconfirmedTran transaction = Unmap.get ( key );
            if ( transaction.getTransType ( ) != 1 ) {
                if ( map.get ( transaction.getTransFrom ( ) + "_" + transaction.getTokenName ( ).toLowerCase ( ) ) != null ) {//支出
                    String value = map.get ( transaction.getTransFrom ( ) + "_" + transaction.getTokenName ( ).toLowerCase ( ) );//收入-支出
                    long valueNow = Long.valueOf ( value.split ( "," )[1] ).longValue ( ) + transaction.getTransValue ( );
                    map.put ( transaction.getTransFrom ( ) + "_" + transaction.getTokenName ( ).toLowerCase ( ), value.split ( "," )[0] + "," + valueNow );
                }
                if ( map.get ( transaction.getTransFrom ( ) + "_" + transaction.getTokenName ( ).toLowerCase ( ) ) == null ) {//支出
                    long value = transaction.getTransValue ( );
                    map.put ( transaction.getTransFrom ( ) + "_" + transaction.getTokenName ( ).toLowerCase ( ), 0 + "," + value );
                }
            }
            if ( map.get ( transaction.getTransTo ( ) + "_" + transaction.getTokenName ( ).toLowerCase ( ) ) != null ) {//收入
                String value = map.get ( transaction.getTransTo ( ) + "_" + transaction.getTokenName ( ).toLowerCase ( ) );////收入-支出
                long valueNow = Long.valueOf ( value.split ( "," )[0] ).longValue ( ) + transaction.getTransValue ( );
                map.put ( transaction.getTransTo ( ) + "_" + transaction.getTokenName ( ).toLowerCase ( ), valueNow + "," + value.split ( "," )[1] );
            }
            if ( map.get ( transaction.getTransTo ( ) + "_" + transaction.getTokenName ( ).toLowerCase ( ) ) == null ) {//收入
                long value = transaction.getTransValue ( );
                map.put ( transaction.getTransTo ( ) + "_" + transaction.getTokenName ( ).toLowerCase ( ), value + "," + 0 );
            }
        }
        for (String key : Unmap.keySet ( )) {
            UnconfirmedTran transaction = Unmap.get ( key );
            if ( map.get ( transaction.getTransFrom ( ) + "_" + Constants.PTN ) != null ) {//支出
                String value = map.get ( transaction.getTransFrom ( ) + "_" + Constants.PTN );//收入-支出
                long valueNow = Long.valueOf ( value.split ( "," )[1] ).longValue ( ) + transaction.getFee ( );
                map.put ( transaction.getTransFrom ( ) + "_" + Constants.PTN, value.split ( "," )[0] + "," + valueNow );
            }
            if ( map.get ( transaction.getTransFrom ( ) + "_" + Constants.PTN ) == null ) {//收入
                map.put ( transaction.getTransFrom ( ) + "_" + Constants.PTN, 0 + "," + transaction.getFee ( ) );
            }
        }
        return map;
    }


    /**
     * 统计未确认流水from的金额
     */
    public static Map<String, Long> getAccountFromTransValue(List<UnconfirmedTran> list) {
        Map<String, Long> map = new HashMap ( );//key:XXX,value:支出
        for (UnconfirmedTran transaction : list) {
            if ( transaction.getTransType ( ) != 1 ) {
                if ( map.get ( transaction.getTransFrom ( ) + "_" + transaction.getTokenName ( ).toLowerCase ( ) ) != null ) {//支出
                    Long value = map.get ( transaction.getTransFrom ( ) + "_" + transaction.getTokenName ( ).toLowerCase ( ) );//支出
                    long valueNow = value + transaction.getTransValue ( );
                    map.put ( transaction.getTransFrom ( ) + "_" + transaction.getTokenName ( ).toLowerCase ( ), valueNow );
                }
                if ( map.get ( transaction.getTransFrom ( ) + "_" + transaction.getTokenName ( ).toLowerCase ( ) ) == null ) {//支出
                    long value = transaction.getTransValue ( );
                    map.put ( transaction.getTransFrom ( ) + "_" + transaction.getTokenName ( ).toLowerCase ( ), value );
                }
            }
        }
        for (UnconfirmedTran transaction : list) {
            if ( map.get ( transaction.getTransFrom ( ) + "_" + Constants.PTN ) != null ) {//支出
                Long value = map.get ( transaction.getTransFrom ( ) + "_" + Constants.PTN );
                long valueNow = value + transaction.getFee ( );
                map.put ( transaction.getTransFrom ( ) + "_" + Constants.PTN, valueNow );
            }
            if ( map.get ( transaction.getTransFrom ( ) + "_" + Constants.PTN ) == null ) {//支出
                map.put ( transaction.getTransFrom ( ) + "_" + Constants.PTN, transaction.getFee ( ) );
            }
        }
        return map;
    }


}
