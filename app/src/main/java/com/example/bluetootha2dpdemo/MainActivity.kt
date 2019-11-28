package com.example.bluetootha2dpdemo

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.text.TextUtils
import android.util.Log
import android.view.View
import com.sf.lib_scannerbluetooth.BtDevice
import com.sf.lib_scannerbluetooth.headset.HeadsetManager
import com.sf.lib_scannerbluetooth.headset.HeadsetManager.BT_STATE_CONNECTED
import com.sf.lib_scannerbluetooth.headset.HeadsetManager.BT_STATE_CONNECTING
import com.sf.lib_scannerbluetooth.listener.ConnectObserver
import kotlinx.android.synthetic.main.activity_main.*
import java.util.ArrayList

/**
 * @Author: Jinhuan.Li
 * @Date: 2019/11/28
 * @Des: //TODO  抽取demo
 */
class MainActivity : AppCompatActivity(),HeadsetManager.DiscoverCallback, ConnectObserver {


    private var mDeviceAdapter: DeviceAdapter? = null
    private var mLinearLayoutManager: LinearLayoutManager? = null

    private var deviceList: MutableList<BtDevice> = ArrayList()
    private val REQUEST_ENABLE_BT = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initBluetooth()
        initUI()
        initListener()
        getDevices()
    }


    private fun initBluetooth() {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
        } else {
            HeadsetManager.getInstance(this).init()
        }
        HeadsetManager.getInstance(this).registDiscoverCallback(this)
        HeadsetManager.getInstance(this).addObserver(this)
    }

    private fun initUI() {
        mLinearLayoutManager = LinearLayoutManager(this)
        mLinearLayoutManager?.orientation = LinearLayoutManager.VERTICAL
        device_list.layoutManager = mLinearLayoutManager
        mDeviceAdapter = DeviceAdapter()
        device_list.adapter = mDeviceAdapter

    }


    private fun initListener(){
        //扫描，扫描后，在下方封装的回调方法里面接收
        scanBtn.setOnClickListener {
            HeadsetManager.getInstance(this).startDiscover()
        }
        //断连
        disconn_btn.setOnClickListener{
        }
        //item 点击,连接
        mDeviceAdapter?.onItemClickListner=object:DeviceAdapter.OnItemClickListener{
            override fun onItemClick(v: View?, position: Int?) {
                stateTv.text = "当前状态：连接中"
                connect((position?.let { mDeviceAdapter?.datas?.get(it) } as BtDevice).macAddr)
            }

        }
    }

    private fun getDevices() {
        deviceList.clear()
        deviceList = getPairedDevices() as MutableList<BtDevice>
        mDeviceAdapter?.datas=deviceList
    }

    fun connect(address: String) {
        HeadsetManager.getInstance(this).startConn(address)
    }

    private fun getPairedDevices(): List<BtDevice> {
        val deviceSet = HeadsetManager.getInstance(this).pairedDeviceList
        for (i in deviceSet!!.indices) {
            Log.d("demo" , " 添加已配对 耳机设备 device=" + deviceSet[i].name + " mac=" + deviceSet[i].macAddr)
        }
        return deviceSet
    }

    /**
     * =====================================
     *
     *  以下为蓝牙监听回调
     *
     * =====================================
     */
    override fun onStartDiscover() {
        if (BuildConfig.DEBUG) {
            Log.d("demo"," onStartDiscover ->")
        }
        scanStateTv.text="开始扫描"
    }

    override fun onNewDevice(btDevice: BtDevice?) {
        if (BuildConfig.DEBUG) {
            Log.d("demo"," onNewDevice ->")
        }
        scanStateTv.text="扫描到新设备：${btDevice?.macAddr}"
        addDeviceIntoList(btDevice)
    }

    override fun onPariedDevice(btDevice: BtDevice?) {
        if (BuildConfig.DEBUG) {
            Log.d("demo"," onPariedDevice ->" + btDevice?.getName() + " \t" + btDevice?.getMacAddr())
        }
    }

    override fun onCompelete() {
        if (BuildConfig.DEBUG) {
            Log.d("demo"," onCompelete ->")
        }
        scanStateTv.text="扫描完成"
    }

    override fun onStatus(currentConnectDevice: BtDevice?, currentStatus: Int) {
        if (currentConnectDevice == null) {
            stateTv.text = "当前状态：未连接"
            return
        }
        if (currentStatus == BT_STATE_CONNECTED) {
            Log.d("demo", " 蓝牙耳机已连接 mac=" + currentConnectDevice.macAddr)
            stateTv.text = "当前状态：已连接：\n ${currentConnectDevice.name} ${currentConnectDevice.macAddr}"
        } else if (currentStatus == BT_STATE_CONNECTING) {
            Log.d("demo", " 蓝牙耳机连接中 mac=" + currentConnectDevice.macAddr)
            stateTv.text = "当前状态：连接中:\n ${currentConnectDevice.name} ${currentConnectDevice.macAddr}"
        } else {
            stateTv.text = "当前状态：未连接"
        }
    }


    /**
     * 设备列表，过滤相同的
     */
    private fun addDeviceIntoList(device: BtDevice?) {
        if (device == null) return
        val oldDeviceList = mDeviceAdapter?.datas
        if (oldDeviceList == null || oldDeviceList!!.size == 0) {
            mDeviceAdapter?.addDevice(device)
        } else {
            var flag = false
            for (i in oldDeviceList!!.indices) {
                //过滤删掉相同设备： mac相同的
                if (TextUtils.equals(oldDeviceList!!.get(i).getMacAddr(), device.macAddr)) {
                    flag = true
                    break
                }
            }
            if (!flag) {
                mDeviceAdapter?.addDevice(device)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        HeadsetManager.getInstance(this).stopDiscover()
        HeadsetManager.getInstance(this).unRegistDiscoverCallback()
        HeadsetManager.getInstance(this).removeObserver(this)
    }
}
