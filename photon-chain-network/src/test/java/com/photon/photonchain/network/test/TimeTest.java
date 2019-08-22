package com.photon.photonchain.network.test;

import com.photon.photonchain.network.utils.FoundryUtils;
import java.util.Calendar;
import org.spongycastle.util.encoders.Hex;

/**
 * @author Wu Created by SKINK on 2018/2/28.
 */
public class TimeTest {

  public static void main(String[] args) {
  }

  private int getDiffYear(long genesis,long current){
    //1519747200000 2018年2月28日0时0分0秒
    //1488211200000 2017年2月28日0时0分0秒
    //1456588800000 2016年2月28日0时0分0秒
    Calendar calendarOne = Calendar.getInstance();
    calendarOne.setTimeInMillis(genesis);
    Calendar calendarTwo = Calendar.getInstance();
    calendarTwo.setTimeInMillis(current);
    int year1 = calendarOne.get(Calendar.YEAR);
    int year2 = calendarTwo.get(Calendar.YEAR);
   return (year2-year1);
  }

}
