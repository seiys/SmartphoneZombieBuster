package com.example.smartphone_zombie_buster;
/*参考: https://iyemon018.hatenablog.com/entry/2017/12/28/012030*/
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class WakeLockBroadcastReceiver extends BroadcastReceiver {

    private WakeLockListener listener;

    public WakeLockBroadcastReceiver(WakeLockListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_SCREEN_ON)) {
            this.listener.onScreenOn();
        } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
            this.listener.onScreenOff();
        }
    }
}
