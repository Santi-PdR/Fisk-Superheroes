package com.fiskmods.heroes.pack.js;

/** Read-only view of a supported legacy rule passed to setRuleValueModifier. */
public final class JSRule
{
    private final String name;
    private final Number value;
    private final Number defaultValue;
    private final boolean integer;

    public JSRule(String name, Number value, Number defaultValue, boolean integer)
    {
        this.name = name;
        this.value = value;
        this.defaultValue = defaultValue;
        this.integer = integer;
    }

    public String name() { return name; }
    public Number value() { return value; }
    public Number defaultValue() { return defaultValue; }
    public boolean isBoolean() { return false; }
    public boolean isInteger() { return integer; }
    public boolean isFloat() { return !integer; }
    public boolean isDouble() { return false; }
}
