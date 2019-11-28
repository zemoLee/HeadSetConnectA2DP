package com.sf.lib_scannerbluetooth.listener;

import com.sf.lib_scannerbluetooth.BtDevice;

public interface ConnectObserver {
    void onStatus(BtDevice currentConnectDevice, int currentStatus);
}
