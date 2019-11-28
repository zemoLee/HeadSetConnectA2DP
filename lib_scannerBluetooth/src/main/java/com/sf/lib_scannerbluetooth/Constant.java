package com.sf.lib_scannerbluetooth;

/**
 * @Author: Jinhuan.Li
 * @Date: 2019/6/28
 * @Des: //TODO
 */
public class Constant {
    // Message types sent from the BluetoothScannerService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothScannerService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String DEVICE_MAC = "device_mac";
    public static final String DEVICE = "device";
    public static final String TOAST = "toast";

    /**
     * intent请求值
     */
    public static final int REQUEST_CONNECT_DEVICE = 1;
    public static final int REQUEST_ENABLE_BT = 2;
}
