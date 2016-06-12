package tjabc.android.com.devicedemo.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Lee on 2016/3/8.
 */
public class GattUpdateReceiver extends BroadcastReceiver {

    public interface GattUpdateImpl {
        void onGatt_CONNECTED_Listener();

        void onGatt_SERVICE_DISCOVERED_Listener();

        void onGatt_AVAILABLE_Listener(String gattStr);

        void onGatt_ELSE_Listener();
    }
    public static GattUpdateImpl gattupdate;

    private String nextItem="";
    private String prevItem="";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i("receiver","action:"+action);

        if(BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)){
            gattupdate.onGatt_CONNECTED_Listener();
        }else if(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)){
            gattupdate.onGatt_SERVICE_DISCOVERED_Listener();
        } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
            nextItem = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
            if (prevItem.equals(nextItem)) {
                prevItem=nextItem;
                return;
            } else {
                gattupdate.onGatt_AVAILABLE_Listener(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                prevItem=nextItem;
            }

        } else {
            gattupdate.onGatt_ELSE_Listener();
        }

    }

}
