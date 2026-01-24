package io.kineticedge.koffset.util.domain;

import java.util.Map;

public class Bad {

    private String missingSetter;
    private String badSet;
    private String nonPublicGetter;
    private String nonPublicSetter;

    private Map<String, String> badMap;

    public String missingSetter() {
        return missingSetter;
    }

    public String getBadGet() {
        throw new RuntimeException("get of BadGet is not supported");
    }

    public void setBadGet(String badGet) {
    }


    public String getBadSet() {
        return badSet;
    }

    public void setBadSet(String badSet) {
        throw new RuntimeException("set of BadSet is not supported");
    }

    String getNonPublicGetter() {

        return nonPublicGetter;
    }

    public void setNonPublicGetter(String nonPublicGetter) {
        this.nonPublicGetter = nonPublicGetter;
    }

    public String getNonPublicSetter() {
        return nonPublicSetter;
    }

    private void setNonPublicSetter(String nonPublicSetter) {
        this.nonPublicSetter = nonPublicSetter;
    }

}
