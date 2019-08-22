package com.photon.photonchain.network.utils;

import com.photon.photonchain.storage.constants.Constants;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * @Author:Lin
 * @Description:
 * @Date:19:26 2018/1/16
 * @Modified by:
 */
public class DateUtil {

    public static final String DATETIME_HOURS_FORMAT = "yyyy-MM-dd HH:00:00";

    private static DateFormat dateTimeHoursFormat = null;

    private static final String WEB_URL = "http://47.90.81.166/";
    private static final String FORMAT_datatime =  "yyyy-MM-dd HH:mm:ss";

    static {
        dateTimeHoursFormat = new SimpleDateFormat ( DATETIME_HOURS_FORMAT);
    }

    public static String stampToDateHours(Long s) {
        String res;
        long lt = new Long ( s );
        Date date = new Date ( lt );
        res = dateTimeHoursFormat.format ( date );
        return res;
    }


    public static long getWebTime() {
        long time = Constants.GENESIS_TIME;
        try {
            URL url = new URL ( WEB_URL );
            URLConnection uc = url.openConnection ( );
            uc.connect ( );
            time = uc.getDate ( );
        } catch (MalformedURLException e) {
            e.printStackTrace ( );
        } catch (IOException e) {
            e.printStackTrace ( );
        }
        return time > Constants.GENESIS_TIME ? time : Constants.GENESIS_TIME;
    }

    public static Date strToDate(String str) {
        try{
            Date date=null;
            SimpleDateFormat formatter=new SimpleDateFormat(FORMAT_datatime);
            date=formatter.parse(str);
            return date;
        }catch (Exception e){
            e.printStackTrace();
        }
        return new Date();
    }

    /**
     * 时间转换成时间戳
     * @param time
     * @return
     */
    public static long dateToTimestamp(String time){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
        try {
            Date date = simpleDateFormat.parse(time);
            long ts = date.getTime() / 1000;
            return ts;
        } catch (ParseException e) {
            return 0;
        }
    }


    /**
     * 时间戳转时间(11位时间戳)
     * @param time
     * @return
     */
    public static String timestampToDate(long time) {
        String dateTime;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(FORMAT_datatime);
        long timeLong = Long.valueOf(time);
        dateTime = simpleDateFormat.format(new Date(timeLong * 1000L));
        return dateTime;
    }

}
