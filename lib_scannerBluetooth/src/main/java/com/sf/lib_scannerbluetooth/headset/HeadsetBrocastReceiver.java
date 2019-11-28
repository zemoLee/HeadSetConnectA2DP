package com.sf.lib_scannerbluetooth.headset;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * 蓝牙耳机广播接收处
 */
public class HeadsetBrocastReceiver extends BroadcastReceiver {
    public static final String TAG = "HeadsetBrocastReceiver";
    private Callback mCallback;

    /**
     * 蓝牙连接状态回调
     */
    public interface Callback {
        void onBluetoothConnected();

        void onBluetoothError();
    }

    /**
     * 从外面向广播对象传递回调
     *
     * @param callback
     * @param c
     */
    public static void register(Callback callback, Context c) {
        c.registerReceiver(new HeadsetBrocastReceiver(callback), getFilter());
    }

    /**
     * 注册广播的action为 连接状态变化action
     *
     * @return
     */
    private static IntentFilter getFilter() {
        return new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
    }

    /**
     * 构造
     *
     * @param callback
     */
    public HeadsetBrocastReceiver(Callback callback) {
        this.mCallback = callback;
    }

    /**
     * 广播接收处
     *
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
            Log.d(TAG, " 没有收到状态变化广播");
            return;
        }
        //获取状态
        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
        switch (state) {
            //连接上
            case BluetoothAdapter.STATE_CONNECTED:
                Log.d(TAG, " 收到状态变化广播=STATE_CONNECTED");
                unregisterReceiver(context, this);
                onConnected();
                break;
            //连接失败或错误
            case BluetoothAdapter.ERROR:
                Log.d(TAG, " 收到状态变化广播=ERROR");
                unregisterReceiver(context, this);
                onBluetoothError();
                break;
        }
    }

    /**
     * 取消注册广播
     *
     * @param c
     * @param receiver
     */
    private static void unregisterReceiver(Context c, BroadcastReceiver receiver) {
        try {
            c.unregisterReceiver(receiver);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 蓝牙已连接，通知回调
     */
    private void onConnected() {
        if (mCallback != null) {
            mCallback.onBluetoothConnected();
        }
    }

    /**
     * 蓝牙连接错误或失败
     */
    private void onBluetoothError() {
        if (mCallback != null) {
            mCallback.onBluetoothError();
        }
    }
}
