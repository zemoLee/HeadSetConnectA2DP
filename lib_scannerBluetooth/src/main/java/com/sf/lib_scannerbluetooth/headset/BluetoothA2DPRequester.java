package com.sf.lib_scannerbluetooth.headset;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

/**
 * @Author: Jinhuan.Li
 * @Date: 2019/8/6
 * @Des: //TODO
 */
public class BluetoothA2DPRequester implements BluetoothProfile.ServiceListener {
    public static final String TAG = BluetoothA2DPRequester.class.getSimpleName();
    private Callback mCallback;
    private BluetoothProfile mA2DProfile;
    private BluetoothAdapter mAdapter;
    private static BluetoothA2DPRequester mInstance;

    private BluetoothA2DPRequester() {

    }

    public static BluetoothA2DPRequester getInstance() {
        if (null == mInstance) {
            synchronized (BluetoothA2DPRequester.class) {
                mInstance = new BluetoothA2DPRequester();
            }
        }
        return mInstance;
    }

    public void setBluetoothA2DPRequesterCallback(BluetoothA2DPRequester.Callback callback) {
        mCallback = callback;
    }


    public void request(Context c, BluetoothAdapter adapter) {
        mAdapter = adapter;
        adapter.getProfileProxy(c, this, BluetoothProfile.A2DP);
    }

    @Override
    public void onServiceConnected(int profile, BluetoothProfile bluetoothProfile) {
        mA2DProfile = bluetoothProfile;
        if (mCallback != null) {
            Log.d(TAG, "A2DP  onServiceConnected");
            mCallback.onA2DPProxyReceived(profile, (BluetoothA2dp) bluetoothProfile);
        }
    }

    @Override
    public void onServiceDisconnected(int i) {
        Log.d(TAG, "A2DP  onServiceDisconnected");
    }

    public interface Callback {
        void onA2DPProxyReceived(int profile, BluetoothA2dp proxy);
    }

    /**
     * 获取A2DP连接状态
     *
     * @return
     */
    public int getA2DPState() {
        if (mAdapter != null) {
            return mAdapter.getProfileConnectionState(BluetoothProfile.A2DP);
        } else {
            return -1;
        }
    }

    /**
     * 获取指定设备A2dp连接状态
     *
     * @param device
     * @return
     */
    public boolean getA2dpState(BluetoothDevice device) {
        if (mA2DProfile != null && mA2DProfile.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED) {
            return true;
        }
        return false;
    }
}
