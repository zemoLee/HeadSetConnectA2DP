package com.sf.lib_scannerbluetooth.pool;

import java.util.concurrent.ExecutorService;

public class BluetoothExecutorTools {
    public static ExecutorService sExecutorService;

    /**
     * 注入全局线程库
     *
     * @param sExecutorService
     */
    public static void setsExecutorService(ExecutorService sExecutorService) {
        BluetoothExecutorTools.sExecutorService = sExecutorService;
    }
}
