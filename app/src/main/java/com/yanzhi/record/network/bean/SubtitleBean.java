package com.yanzhi.record.network.bean;

import java.util.List;

public class SubtitleBean {

    /**
     * bg : 0
     * ed : 1890
     * nc : 1.0
     * onebest : 嗯
     * si : 0
     * speaker : 0
     * wordsResultList : [{"alternativeList":[],"wc":"1.0000","wordBg":"11","wordEd":"164","wordsName":"嗯","wp":"s"}]
     */

    private String bg;//句子相对于本音频的起始时间，单位为ms
    private String ed;//句子相对于本音频的终止时间，单位为ms
    private String nc;//句子置信度，范围为[0,1]
    private String onebest;//句子内容
    private String si;//句子位置，从0开始累加
    private String speaker;//说话人编号(数字“1”和“2”为不同说话人，电话专用版功能)
    private List<WordsResultListBean> wordsResultList;//分词内容

    public String getBg() {
        return bg;
    }

    public void setBg(String bg) {
        this.bg = bg;
    }

    public String getEd() {
        return ed;
    }

    public void setEd(String ed) {
        this.ed = ed;
    }

    public String getNc() {
        return nc;
    }

    public void setNc(String nc) {
        this.nc = nc;
    }

    public String getOnebest() {
        return onebest;
    }

    public void setOnebest(String onebest) {
        this.onebest = onebest;
    }

    public String getSi() {
        return si;
    }

    public void setSi(String si) {
        this.si = si;
    }

    public String getSpeaker() {
        return speaker;
    }

    public void setSpeaker(String speaker) {
        this.speaker = speaker;
    }

    public List<WordsResultListBean> getWordsResultList() {
        return wordsResultList;
    }

    public void setWordsResultList(List<WordsResultListBean> wordsResultList) {
        this.wordsResultList = wordsResultList;
    }

    public static class WordsResultListBean {
        /**
         * alternativeList : []
         * wc : 1.0000
         * wordBg : 11
         * wordEd : 164
         * wordsName : 嗯
         * wp : s
         */

        private String wc;//词置信度，范围为[0,1]
        private String wordBg;//词相对于本句子的起始帧
        private String wordEd;//词相对于本句子的终止帧
        private String wordsName;
        private String wp;//词属性  s语气词，p标点符号，n正文。
        private List<?> alternativeList;

        public String getWc() {
            return wc;
        }

        public void setWc(String wc) {
            this.wc = wc;
        }

        public String getWordBg() {
            return wordBg;
        }

        public void setWordBg(String wordBg) {
            this.wordBg = wordBg;
        }

        public String getWordEd() {
            return wordEd;
        }

        public void setWordEd(String wordEd) {
            this.wordEd = wordEd;
        }

        public String getWordsName() {
            return wordsName;
        }

        public void setWordsName(String wordsName) {
            this.wordsName = wordsName;
        }

        public String getWp() {
            return wp;
        }

        public void setWp(String wp) {
            this.wp = wp;
        }

        public List<?> getAlternativeList() {
            return alternativeList;
        }

        public void setAlternativeList(List<?> alternativeList) {
            this.alternativeList = alternativeList;
        }
    }
}
