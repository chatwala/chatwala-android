package com.chatwala.android.util;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/16/2014
 * Time: 12:17 PM
 * To change this template use File | Settings | File Templates.
 */
public enum DeliveryMethod {
    SMS(0, "  SMS"),
    CWSMS(1, "  Chatwala SMS"),
    EMAIL(2, "  Email"),
    FB(3, "  Facebook"),
    TOP_CONTACTS(4, "  Top Contacts");

    private int method;
    private String displayString;

    DeliveryMethod(int method, String displayString) {
        this.method = method;
        this.displayString = displayString;
    }

    public int getMethod() {
        return method;
    }

    public String getDisplayString() {
        return displayString;
    }
}
