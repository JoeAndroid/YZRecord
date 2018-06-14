package com.yanzhi.record.utils;

public class Utils {

    /*
     * 毫秒转化时分秒毫秒
     * ms 微秒
     */
    public static String formatTime(long ms) {
        int ss = 1000000;
        int mi = ss * 60;


        long minute = ms / mi;
        long second = (ms - minute * mi) / ss;


        StringBuffer sb = new StringBuffer();

        if (minute < 10) {
            sb.append("0" + minute + ":");
        } else {
            sb.append(minute + ":");
        }
        if (second < 10) {
            sb.append("0" + second);
        } else {
            sb.append(second);
        }

        return sb.toString();
    }
}
