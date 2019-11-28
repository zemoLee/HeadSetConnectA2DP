package com.sf.lib_scannerbluetooth.listener;

public interface ConnectObservable {
    void addObserver(ConnectObserver observer);

    void removeObserver(ConnectObserver observer);

    void notifyObservers();
}
