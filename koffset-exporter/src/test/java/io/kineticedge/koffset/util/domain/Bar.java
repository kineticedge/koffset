package io.kineticedge.koffset.util.domain;

import java.util.Map;

public class Bar {

    private String stringValue;
    private Map<String, String> map;

    private boolean isSomething;

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public Map<String, String> getMap() {
        return map;
    }

    public void setMap(Map<String, String> map) {
        this.map = map;
    }

    public boolean isSomething() {
        return isSomething;
    }

    public void setSomething(boolean something) {
        isSomething = something;
    }
}
