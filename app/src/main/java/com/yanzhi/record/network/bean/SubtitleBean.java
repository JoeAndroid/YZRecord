package com.yanzhi.record.network.bean;

import java.util.List;

public class SubtitleBean {

    /**
     * bg : 120
     * ed : 3150
     * nc : 1.0
     * onebest : 就是测试音频测试音频，
     * si : 0
     * speaker : 0
     * wordsResultList : [{"alternativeList":[],"wc":"1.0000","wordBg":"42","wordEd":"74","wordsName":"就是","wp":"n"},{"alternativeList":[],"wc":"1.0000","wordBg":"74","wordEd":"112","wordsName":"测试","wp":"s"},{"alternativeList":[],"wc":"1.0000","wordBg":"112","wordEd":"189","wordsName":"音频","wp":"s"},{"alternativeList":[],"wc":"1.0000","wordBg":"189","wordEd":"228","wordsName":"测试","wp":"n"},{"alternativeList":[],"wc":"1.0000","wordBg":"228","wordEd":"294","wordsName":"音频","wp":"n"},{"alternativeList":[],"wc":"0.0000","wordBg":"294","wordEd":"294","wordsName":"，","wp":"p"}]
     */

    private String bg;
    private String ed;
    private String nc;
    private String onebest;
    private String si;
    private String speaker;
    private List<WordsResultListBean> wordsResultList;

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
         * wordBg : 42
         * wordEd : 74
         * wordsName : 就是
         * wp : n
         */

        private String wc;
        private String wordBg;
        private String wordEd;
        private String wordsName;
        private String wp;
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
