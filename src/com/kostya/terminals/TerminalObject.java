package com.kostya.terminals;

import com.kostya.serializable.ComPortObject;

import java.io.Serializable;

/**
 * Created by Kostya on 12.07.2016.
 */
public class TerminalObject implements Serializable {
    private static final long serialVersionUID = 7526471155622776149L;
    private Terminals terminal;
    private ComPortObject comPortObject;
    private String ipAddress;

    public TerminalObject(Terminals terminal, ComPortObject comPortObject){
        this.terminal = terminal;
        this.comPortObject = comPortObject;
    }

    public TerminalObject(Terminals terminal){
        this.terminal = terminal;
    }

    public void setComPortObject(ComPortObject comPortObject) {this.comPortObject = comPortObject; }
    public ComPortObject getComPortObject() {return comPortObject;}
    public String getIpAddress() {return ipAddress;}
    public void setIpAddress(String ipAddress) {this.ipAddress = ipAddress;}
}
