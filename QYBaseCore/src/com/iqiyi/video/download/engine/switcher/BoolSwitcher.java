package com.iqiyi.video.download.engine.switcher;

/**
 * 用一个boolean值实现的开关类，实现XSwitcher的接口。
 */
public final class BoolSwitcher implements ISwitcher {

    private volatile boolean on;

    public BoolSwitcher(boolean on) {

        this.on = on;
    }

    @Override
    public boolean isOn() {
        return on;
    }

    public synchronized void turnOn() {

        on = true;

    }

    public synchronized void turnOff() {

        on = false;

    }
}
