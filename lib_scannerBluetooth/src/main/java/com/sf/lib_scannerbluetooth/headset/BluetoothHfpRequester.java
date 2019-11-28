package com.sf.lib_scannerbluetooth.headset;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

/**
 * @Author: Jinhuan.Li
 * @Date: 2019/8/6
 * @Des: //TODO
 */
public class BluetoothHfpRequester implements BluetoothProfile.ServiceListener {
    public static final String TAG = BluetoothHfpRequester.class.getSimpleName();
    private Callback mCallback;
    private BluetoothAdapter mAdapter;
    private BluetoothProfile mHfpProfile;
    private static BluetoothHfpRequester mInstance;

    private BluetoothHfpRequester() {

    }

    public static BluetoothHfpRequester getInstance() {
        if (null == mInstance) {
            synchronized (BluetoothHfpRequester.class) {
                mInstance = new BluetoothHfpRequester();
            }
        }
        return mInstance;
    }

    public void setBluetoothHfpRequester(Callback callback) {
        mCallback = callback;
    }


    public void request(Context c, BluetoothAdapter adapter) {
        mAdapter = adapter;
        adapter.getProfileProxy(c, this, BluetoothProfile.HEADSET);
    }

    @Override
    public void onServiceConnected(int profile, BluetoothProfile bluetoothProfile) {
        mHfpProfile = bluetoothProfile;
        if (mCallback != null) {
            Log.d(TAG, "HFP  onServiceConnected");
            mCallback.onHfpProxyReceived(profile, (BluetoothHeadset) bluetoothProfile);
        }
    }

    @Override
    public void onServiceDisconnected(int i) {
        Log.d(TAG, "HFP  onServiceDisconnected");
    }

    public interface Callback {
        void onHfpProxyReceived(int profile, BluetoothHeadset proxy);
    }

    /**
     * 获取HFP连接状态
     *
     * @return
     */
    public int getHfpState() {
        if (mAdapter != null) {
            return mAdapter.getProfileConnectionState(BluetoothProfile.HEADSET);
        } else {
            return -1;
        }
    }

    /**
     * 获取指定设备的HFP连接状态
     *
     * @param device
     * @return
     */
    public boolean getHfpState(BluetoothDevice device) {
        Log.e(TAG, "hfpProfile_-->" + mHfpProfile);
        if (mHfpProfile != null && mHfpProfile.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED) {
            return true;
        }
        return false;
    }
}
