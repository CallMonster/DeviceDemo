package tjabc.android.com.devicedemo.activity;

import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import tjabc.android.com.devicedemo.R;
import tjabc.android.com.devicedemo.service.BluetoothLeService;
import tjabc.android.com.devicedemo.service.GattUpdateReceiver;
import tjabc.android.com.devicedemo.utils.ConvertUtils;
import tjabc.android.com.devicedemo.utils.SampleGattAttributes;

public class DeviceControlActivity extends AppCompatActivity implements View.OnClickListener,GattUpdateReceiver.GattUpdateImpl {
    private String TAG="device control";
    private final String GATT_PASS_CODE="1234567890";

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    @Bind(R.id.goDevice) Button goDeviceBtn;//配对
    @Bind(R.id.connectBtn) Button connectBtn;//连接
    @Bind(R.id.data_value) TextView data_value;
    @Bind(R.id.stateConnectView) TextView stateConnectView;

    private String mDeviceName;
    private String mDeviceAddress;
    private boolean mConnected = false;//是否已经连接
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics;
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private BluetoothLeService mBluetoothLeService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);
        ButterKnife.bind(this);
        GattUpdateReceiver.gattupdate=this;

        Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        data_value.setText(mDeviceName+"--"+mDeviceAddress);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        goDeviceBtn.setOnClickListener(this);
        connectBtn.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothLeService != null) {
            boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.goDevice://配对
                if (mGattCharacteristics != null) {
                    BluetoothGattCharacteristic characteristic =mGattCharacteristics.get(3).get(0);
                    int charaProp = characteristic.getProperties();
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                        byte[] value = new byte[20];
                        value[0] = (byte) 0x00;
                        byte[] WriteBytes = GATT_PASS_CODE.getBytes();
                        characteristic.setValue(value[0],BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                        characteristic.setValue(WriteBytes);
                        mBluetoothLeService.writeCharacteristic(characteristic);
                    }

                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                        mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                    }


                }
                break;
            case R.id.connectBtn://连接
                mBluetoothLeService.connect(mDeviceAddress);
                break;
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            mBluetoothLeService.connect(mDeviceAddress);
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothLeService = null;
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        GattUpdateReceiver.gattupdate=null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        GattUpdateReceiver.gattupdate=null;
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    /**
     * 更新连接状态
     * @param state
     */
    private void updateConnectionState(String state) {
        if(state!=null){
            stateConnectView.setText(state);
        }
    }

    @Override
    public void onGatt_CONNECTED_Listener() {
        mConnected = true;
        updateConnectionState("已连接");
    }

    @Override
    public void onGatt_SERVICE_DISCOVERED_Listener() {
        displayGattServices(mBluetoothLeService.getSupportedGattServices());
    }

    @Override
    public void onGatt_AVAILABLE_Listener(String gattStr) {
        if (gattStr != null) {
            data_value.setText(gattStr);
        }
    }

    @Override
    public void onGatt_ELSE_Listener() {
        mConnected = false;
        updateConnectionState("未连接");
        data_value.setText("请连接后再试");
    }

    /**
     * 获取蓝牙设备服务信息
     * @param gattServices
     */
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null){
            return;
        }
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);

        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();
            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }
    }

}
