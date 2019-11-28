package com.sf.lib_scannerbluetooth.headset;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Log;

import com.sf.lib_scannerbluetooth.BtDevice;
import com.sf.lib_scannerbluetooth.BuildConfig;
import com.sf.lib_scannerbluetooth.listener.ConnectObservable;
import com.sf.lib_scannerbluetooth.listener.ConnectObserver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


/**
 * @Author: Jinhuan.Li
 * @Date: 2019/6/30
 * @Des: //指环蓝牙管理器
 */
public class HeadsetManager implements BluetoothA2DPRequester.Callback, BluetoothHfpRequester.Callback, HeadsetBrocastReceiver.Callback, ConnectObservable {
    public static final String TAG = HeadsetManager.class.getSimpleName();
    private static volatile HeadsetManager mInstance;
    private Context mContext;
    public BluetoothAdapter mBtAdapter;
    public static boolean isConnect;
    /**
     * 连接状态
     */
    public static final int BT_STATE_NONE = 0;
    public static final int BT_STATE_LISTEN = 1;
    public static final int BT_STATE_CONNECTING = 2;
    public static final int BT_STATE_CONNECTED = 3;
    /**
     * 连接成功的设备
     */
    private BtDevice mConnectedDevice = null;
    /**
     * 连接中的设备
     */
    private BtDevice mConnectingDevice = null;
    /**
     * 当前连接状态
     */
    private int mConnectStatus = 0;
    /**
     * 监听器
     */
    private List<ConnectObserver> observers = new ArrayList<>();

    private BluetoothProfile mA2dp;
    private BluetoothProfile mHfp;

    private HeadsetManager(Context context) {
        mContext = context.getApplicationContext();

    }

    public static HeadsetManager getInstance(Context context) {
        if (null == mInstance) {
            synchronized (HeadsetManager.class) {
                mInstance = new HeadsetManager(context);
            }
        }
        return mInstance;
    }


    /**
     * 发现蓝牙设备的回调
     */
    public interface DiscoverCallback {
        void onStartDiscover();

        void onNewDevice(BtDevice btDevice);

        void onPariedDevice(BtDevice btDevice);

        void onCompelete();
    }

    private DiscoverCallback mDiscoverCallback;

    public void registDiscoverCallback(DiscoverCallback mDiscoverCallback) {
        this.mDiscoverCallback = mDiscoverCallback;
    }

    public void unRegistDiscoverCallback() {
        this.mDiscoverCallback = null;
    }

    /**
     * @return
     * @Author Jinhuan.Li
     * @method
     * @Params
     * @Description: 方法描述：初始化蓝牙,1.注册蓝牙发现设备的广播，2，注册蓝牙扫描设备完毕的广播
     */
    public void init() {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        registDiscover();
        registStateChange();
        //蓝牙可用
        if (mBtAdapter.isEnabled()) {
            startConnectA2dpProxy();
        }
        //如果蓝牙打开且可用，注册连接状态广播监听
        if (mBtAdapter.enable()) {
            HeadsetBrocastReceiver.register(this, mContext);

        }

    }

    private void registDiscover() {
        //注册扫描发现蓝牙设备的广播
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mContext.registerReceiver(mReceiver, filter);
        //注册蓝牙完成扫描的广播
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mContext.registerReceiver(mReceiver, filter);
    }


    private void registStateChange() {
        IntentFilter filter = new IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        mContext.registerReceiver(mStateReceiver, filter);
    }

    /**
     * @return
     * @Author Jinhuan.Li
     * @method
     * @Params
     * @Description: 方法描述：开始扫描设备
     */
    public void startDiscover() {
        if (BuildConfig.DEBUG) Log.d(TAG, "--- ON startDiscover ---");
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }
        mBtAdapter.startDiscovery();
        if (mDiscoverCallback != null) {
            mDiscoverCallback.onStartDiscover();
        }
    }

    /**
     * @return
     * @Author Jinhuan.Li
     * @method
     * @Params
     * @Description: 方法描述： 停止扫描
     */
    public void stopDiscover() {
        if (BuildConfig.DEBUG) Log.d(TAG, "--- ON stopDiscover ---");
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
        mContext.unregisterReceiver(mReceiver);
    }

    /**
     * @return
     * @Author Jinhuan.Li
     * @method
     * @Params
     * @Description: 方法描述：发起连接，连接到目标地址的远程设备
     */
    public void startConn(String addr) {
        if (mBtAdapter != null) {
            BluetoothDevice device = mBtAdapter.getRemoteDevice(addr);
            mConnectingDevice = new BtDevice(device.getName(), device.getAddress(), device);
        }
        onBluetoothConnected();
    }


    private BtDevice getmConnectingDevice() {
        return mConnectingDevice;
    }


    /**
     * @return
     * @Author Jinhuan.Li
     * @method
     * @Params
     * @Description: 方法描述：已配对的设备，添加到新的设备bean中去
     */
    public List<BtDevice> getPairedDeviceList() {
        if (mBtAdapter == null) return null;
        List<BluetoothDevice> pairedDevices = new ArrayList<>(mBtAdapter.getBondedDevices());
        List<BtDevice> BtDeviceList = new ArrayList<>();
        if (pairedDevices.size() <= 0) return BtDeviceList;
        for (BluetoothDevice device : pairedDevices) {
            String deviceName = device.getName();
            String deviceMac = device.getAddress();
                BtDevice btnDvice = new BtDevice(device.getName().trim(), device.getAddress().trim(), device);
                btnDvice.setPaired(true);
                Log.d(TAG, "获取已配对 耳机设备：转为btdevice name=" + deviceName + " mac=" + deviceMac);
                BtDeviceList.add(btnDvice);

        }
        return BtDeviceList;
    }


    /**
     * 蓝牙发现服务广播
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            /**
             * 发现蓝牙设备
             */
            BtDevice btnDvice;
            String deviceName = "";
            String deviceMac = "";
            if (device != null) {
                deviceName = device.getName();
                deviceMac = device.getAddress();
            }
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.d(TAG, "广播：搜到 耳机设备 ");
                    btnDvice = new BtDevice(TextUtils.isEmpty(deviceName) ? "" : deviceName.trim(), TextUtils.isEmpty(deviceMac) ? "" : deviceMac.trim(), device);
                    //已配对的设备
                    if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        if (mDiscoverCallback != null) {
                            Log.d(TAG, "广播：搜到已配对 耳机设备 name=" + deviceName + " mac=" + deviceMac);
                            mDiscoverCallback.onPariedDevice(btnDvice);
                        }
                    }
                    //新设备，没有配对过的
                    else {
                        if (mDiscoverCallback != null) {
                            Log.d(TAG, "广播：搜到新 耳机设备 name=" + deviceName + " mac=" + deviceMac);
                            mDiscoverCallback.onNewDevice(btnDvice);
                        }
                    }

            }
            /**
             * 搜索蓝牙设备停止
             */
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (mDiscoverCallback != null) {
                    mDiscoverCallback.onCompelete();
                }
            }
        }
    };


    /**
     * 蓝牙状态连接广播
     */
    private final BroadcastReceiver mStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mConnectingDevice = new BtDevice(device.getName(), device.getAddress(), device);
                int state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_DISCONNECTED);
                switch (state) {
                    //断开
                    case BluetoothA2dp.STATE_DISCONNECTED:
                        Log.d(TAG, "STATE_DISCONNECTED");
                        onUnConnect();
                        break;
                    //连接中
                    case BluetoothA2dp.STATE_CONNECTING:
                        Log.d(TAG, "STATE_CONNECTING");
                        onConnecting();
                        break;
                    //已连接
                    case BluetoothA2dp.STATE_CONNECTED:
                        Log.d(TAG, "STATE_CONNECTED");
                        BtDevice connectedDevice = new BtDevice(device.getName(), device.getAddress(), device);
                        setmConnectedDevice(connectedDevice);
                        onConnected();
                        break;
                    //断开中
                    case BluetoothA2dp.STATE_DISCONNECTING:
                        Log.d(TAG, "STATE_DISCONNECTING");
                        onUnConnect();
                        break;
                    default:
                        break;
                }
            }
        }
    };

    private void onConnected() {
        Log.d(TAG, "耳机onConnected");
        isConnect = true;
        mConnectStatus = BT_STATE_CONNECTED;
        notifyObservers();

    }

    private void onConnecting() {
        Log.d(TAG, "耳机onConnecting");
        isConnect = false;
        mConnectStatus = BT_STATE_CONNECTING;
        notifyObservers();
    }

    private void onUnConnect() {
        Log.d(TAG, "耳机onUnConnect");
        isConnect = false;
//        mConnectedDevice = null;
        mConnectStatus =BT_STATE_NONE;
        notifyObservers();
    }

    private void onListenConnect() {
        Log.d(TAG, "onListenConnect");
        isConnect = false;
//        mConnectedDevice = null;
        mConnectStatus = BT_STATE_LISTEN;
        notifyObservers();
    }


    /**
     * 当前连接成功的设备
     *
     * @return
     */
    public BtDevice getmConnectedDevice() {
        return mConnectedDevice;
    }

    private void setmConnectedDevice(BtDevice mConnectedDevice) {
        this.mConnectedDevice = mConnectedDevice;
    }


    /**
     * 断开a2dp设备
     *
     * @param device
     */
    private void disConnectA2dp(BluetoothDevice device) {
        Log.d(TAG, "disConnectA2dp=" + device.getAddress());
        if (mConnectedDevice != null) {
            setPriority(mConnectedDevice.getBluetoothDevice(), 0);
        }
        try {
            Method connectMethod = BluetoothA2dp.class.getMethod("disconnect",
                    BluetoothDevice.class);
            if (mA2dp != null) {
                connectMethod.invoke(mA2dp, device);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 断开hfp
     *
     * @param device
     */
    private void disconnectHfp(BluetoothDevice device) {
        if (mHfp == null) return;
        Method m = null;
        Log.d(TAG, "disconnectHfp=" + device.getAddress());
        try {
            m = mHfp.getClass().getDeclaredMethod("disconnect", BluetoothDevice.class);
            m.setAccessible(true);
            m.invoke(mHfp, device);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void setPriority(BluetoothDevice device, int priority) {
        if (mA2dp == null) return;
        try {
            Method connectMethod = BluetoothA2dp.class.getMethod("setPriority",
                    BluetoothDevice.class, int.class);
            if (mA2dp != null) {
                connectMethod.invoke(mA2dp, device, priority);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断连接状态
     *
     * @param device
     * @return
     */
    public boolean isA2dpConnect(BluetoothDevice device) {
        int state = -1;
        if (mA2dp != null && device != null) {
            state = mA2dp.getConnectionState(device);
        }
        return state == BluetoothA2dp.STATE_CONNECTED;
    }


    @Override
    public void addObserver(ConnectObserver observer) {
        if (this.observers != null && observer != null)
            this.observers.add(observer);
    }

    @Override
    public void removeObserver(ConnectObserver observer) {
        if (this.observers != null && observer != null)
            this.observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        if (observers != null && observers.size() > 0) {
            for (ConnectObserver listner : observers) {
                listner.onStatus(mConnectedDevice, mConnectStatus);
            }
        }
    }

    /**
     * 获取A2dp连接方法
     *
     * @return
     */
    private Method getA2dpConnectMethod() {
        try {
            return BluetoothA2dp.class.getDeclaredMethod("connect", BluetoothDevice.class);
        } catch (NoSuchMethodException ex) {
            Log.e(TAG, "没有找到a2dp的连接方法");
            return null;
        }
    }

    /**
     * 获取HFP连接方法
     *
     * @return
     */
    private Method getHfpConnectMethod() {
        try {
            return BluetoothHeadset.class.getDeclaredMethod("connect", BluetoothDevice.class);
        } catch (NoSuchMethodException ex) {
            Log.e(TAG, "没有找到a2dp的连接方法");
            return null;
        }
    }



    public void startConnectA2dpProxy() {
        onBluetoothConnected();
    }

    /**
     * 连接a2dp通道，通过反射调用
     *
     * @param device
     */
    private void connectDeviceByA2dp(BluetoothDevice device) {
        Log.d(TAG, "connectDeviceByA2dp=" + device.getAddress());
        if (mA2dp == null) {
            onUnConnect();
            return;
        }
        if (isA2dpConnect(device)) {
            onConnected();
            return;
        }
        Method connMethod = getA2dpConnectMethod();
        if (connMethod == null || device == null) return;
        //反射调用a2dp连接
        try {
            Log.d(TAG, "开始蓝牙耳机连接,调用A2dp反射进行连接");
            connMethod.setAccessible(true);
            connMethod.invoke(mA2dp, device);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param device
     */
    private void connectDeviceByHfp(BluetoothDevice device) {
        Log.d(TAG, "connectDeviceByHfp=" + device.getAddress());
        if (mHfp == null) {
            onUnConnect();
            return;
        }
        if (BluetoothHfpRequester.getInstance().getHfpState(device)) {
            return;
        }
        Method connMethod = getHfpConnectMethod();
        if (connMethod == null || device == null) return;
        try {
            Log.d(TAG, "开始蓝牙耳机连接,调用A2dp反射进行连接");
            connMethod.setAccessible(true);
            connMethod.invoke(mHfp, device);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * 当获取到了A2dp协议的代理对象，开始调用A2dp连接
     *
     * @param profile
     * @param proxy
     */
    @Override
    public void onA2DPProxyReceived(int profile, BluetoothA2dp proxy) {
        Log.d(TAG, "获取到A2DP对象，准备连接");
//        if (profile != BluetoothProfile.A2DP) return;
        mA2dp = proxy;
        BtDevice device = getmConnectingDevice();
        if (device == null) {
            Log.e(TAG, "发起a2dp连接前，发现当前正在连接的设备为空，不予连接！");
            return;
        }
        connectDeviceByA2dp(device.getBluetoothDevice());
    }

    /**
     * 获取到hfp协议代理对象
     *
     * @param profile
     * @param proxy
     */
    @Override
    public void onHfpProxyReceived(int profile, BluetoothHeadset proxy) {
        mHfp = proxy;
        BtDevice device = getmConnectingDevice();
        if (device == null) {
            Log.e(TAG, "发起Hfp连接前，发现当前正在连接的设备为空，不予连接！");
            return;
        }
        connectDeviceByHfp(device.getBluetoothDevice());
    }


    /**
     * 蓝牙连接上，开始获取a2dp音频通道协议对象，开始连接，回调onA2DPProxyReceived 接口
     */
    @Override
    public void onBluetoothConnected() {
        Log.d(TAG, "蓝牙耳机A2dp开始连接......");
        BluetoothA2DPRequester.getInstance().setBluetoothA2DPRequesterCallback(this);
        BluetoothA2DPRequester.getInstance().request(mContext, mBtAdapter);
        BluetoothHfpRequester.getInstance().setBluetoothHfpRequester(this);
        BluetoothHfpRequester.getInstance().request(mContext, mBtAdapter);
    }

    /**
     * 蓝牙连接失败或者错误
     */
    @Override
    public void onBluetoothError() {
        Log.d(TAG, "蓝牙耳机连接失败或者错误");
    }


    /**
     * 移除绑定
     *
     * @param btClass
     * @param btDevice
     * @return
     * @throws
     */
    public boolean removeBond(Class btClass, BluetoothDevice btDevice) throws Exception {
        Log.d(TAG, "removeBond=" + btDevice.getAddress());
        Method removeBondMethod = btClass.getMethod("removeBond");
        Boolean returnValue = (Boolean) removeBondMethod.invoke(btDevice);
        return returnValue.booleanValue();
    }
}
