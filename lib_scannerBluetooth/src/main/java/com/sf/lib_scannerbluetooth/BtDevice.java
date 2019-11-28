package com.sf.lib_scannerbluetooth;

import android.bluetooth.BluetoothDevice;

import java.io.Serializable;

/**
 * @Author: Jinhuan.Li
 * @Date: 2019/11/29
 * @Des: //蓝牙设备封装类
 */
public class BtDevice implements Serializable {
    private int connStatus;
    private String name;
    private String macAddr;
    private boolean isPaired;
    private BluetoothDevice bluetoothDevice;

    public BtDevice() {
    }

    public BtDevice(String name, String macAddr, BluetoothDevice bluetoothDevice) {
        this.name = name;
        this.macAddr = macAddr;
        this.bluetoothDevice = bluetoothDevice;
    }

    public int getConnStatus() {
        return connStatus;
    }

    public void setConnStatus(int connStatus) {
        this.connStatus = connStatus;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMacAddr() {
        return macAddr;
    }

    public void setMacAddr(String macAddr) {
        this.macAddr = macAddr;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public boolean isPaired() {
        return isPaired;
    }

    public void setPaired(boolean paired) {
        isPaired = paired;
    }


}
