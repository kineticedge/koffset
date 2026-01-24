package io.kineticedge.koffset.util.domain;

import io.kineticedge.koffset.util.domain.sub.Sub;

import java.util.concurrent.atomic.AtomicLong;

public class Foo {
    private String stringValue;
    private Integer integerValue;
    private Long longValue;
    private Boolean booleanValue;
    private Double doubleValue;
    private Bar bar;
    private Baz baz;
    private Sub sub;
    private AtomicLong external;

    // edge cases
    private String is;
    private String set;
    private String get;

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public Integer getIntegerValue() {
        return integerValue;
    }

    public void setIntegerValue(Integer integerValue) {
        this.integerValue = integerValue;
    }

    public Long getLongValue() {
        return longValue;
    }

    public void setLongValue(Long longValue) {
        this.longValue = longValue;
    }

    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public Double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public Bar getBar() {
        return bar;
    }

    public void setBar(Bar bar) {
        this.bar = bar;
    }

    public Baz getBaz() {
        return baz;
    }

    public void setBaz(Baz baz) {
        this.baz = baz;
    }

    public String is() {
        return is;
    }

    public void is(String is) {
        this.is = is;
    }

    public String set() {
        return set;
    }

    public void set(String set) {
        this.set = set;
    }

    public String get() {
        return get;
    }

    public void get(String get) {
        this.get = get;
    }

    public Sub getSub() {
        return sub;
    }

    public void setSub(Sub sub) {
        this.sub = sub;
    }

    public AtomicLong getExternal() {
        return external;
    }

    public void setExternal(AtomicLong external) {
        this.external = external;
    }
}
