package com.atlas.bleserver;

import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

import android.Manifest;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity {
    
    private static final String TAG = MainActivity.class.getSimpleName();
    private BluetoothAdapter mBluetoothAdapter = null;

    public static final UUID TEMPERATURE_SERVICE_UUID = UUID.fromString("10000000-0000-1000-8000-00805f9b34fb"); //service uuid
    public static final UUID CHAR_READ_NOTIFY_UUID = UUID.fromString("10000001-0000-1000-8000-00805f9b34fb");//service-chara1 uuid
    public static final UUID DESC_NOTITY_UUID = UUID.fromString("10000011-0000-0000-0000-000000000000");//chara1-desc uuid
    public static final UUID WRITE_CHAR_UUID = UUID.fromString("10000002-0000-1000-8000-00805f9b34fb");//service-chara2 uuid
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser; 
    private BluetoothGattServer mBluetoothGattServer;
    
    private static final String weather[] = {"sunny", "overcast", "cloudy", "drizzle", "stormy"};
    private static int mWeaterIndex = 0;
    private TextView mText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mText = (TextView) findViewById(R.id.weather);
        mText.setText("New Weather: " + weather[mWeaterIndex]);
        
      //����λ��Ȩ��
//        requestPermissions();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.d(TAG, "mBluetoothAdapter = " + mBluetoothAdapter);

        //���ù㲥
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) //���ù㲥ģʽ
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH) //���÷��͹���
                .setConnectable(true) //�ܷ����ӣ��㲥��Ϊ�����ӹ㲥�Ͳ������ӹ㲥
                .build();
        //���ù㲥����
        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true) //������������
                .setIncludeTxPowerLevel(true) //�������书�ʼ���
                .addManufacturerData(1, new byte[]{1, 2, 3}) //�豸��������,������Ϊ�豸��������
                .build();
        //����ɨ����Ӧ���ݣ���Ϊ�ͻ���ɨ��(����ǰ)����Ӧ
        AdvertiseData scanResponse = new AdvertiseData.Builder()
                .addManufacturerData(2, new byte[]{1, 2, 3}) //�豸�������ݣ��Զ���
                .addServiceUuid(new ParcelUuid(TEMPERATURE_SERVICE_UUID)) //����UUID
                .build();

        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        Log.d(TAG, "mBluetoothLeAdvertiser = " + mBluetoothLeAdvertiser);
        //����㲥
        mBluetoothLeAdvertiser.startAdvertising(settings, advertiseData, scanResponse, mAdvertiseCallback);

        // =============����BLE���������=====================================================================================
        BluetoothGattService service = new BluetoothGattService(TEMPERATURE_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        //���֧�ֶ���notify������
        BluetoothGattCharacteristic characteristicRead = new BluetoothGattCharacteristic(CHAR_READ_NOTIFY_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ);
        characteristicRead.addDescriptor(new BluetoothGattDescriptor(DESC_NOTITY_UUID, BluetoothGattCharacteristic.PERMISSION_WRITE));
        service.addCharacteristic(characteristicRead);
        //���֧��д������
        BluetoothGattCharacteristic characteristicWrite = new BluetoothGattCharacteristic(WRITE_CHAR_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);
        service.addCharacteristic(characteristicWrite);
        mBluetoothGattServer = ((BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE)).openGattServer(this, mBluetoothGattServerCallback);
        mBluetoothGattServer.addService(service);
        
        

    }

   
    // BLE�㲥Callback
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d(TAG, "BLE advertise start succeed");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.d(TAG, "BLE advertise start failed, errorCode = " + errorCode);
        }
    };

    // BLE�����Callback
    private BluetoothGattServerCallback mBluetoothGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            Log.d(TAG, "onConnectionStateChange, device = " + device + ", status = " + status + ", newState = " + newState);
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            Log.d(TAG, "onServiceAdded, status = " + status + ", service, uuid = " + service.getUuid());
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicReadRequest "  + ", requestId = " + requestId + ", chara = " + characteristic);
            Random rand = new Random();
            mWeaterIndex = rand.nextInt(5);
            Log.d(TAG, "onCharacteristicReadRequest, mWeaterIndex = " + mWeaterIndex);
//            mText.setText("New Weather: " + weather[mWeaterIndex]);
            byte[] by = {(byte) mWeaterIndex};
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, by);// ��Ӧ�ͻ���
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] requestBytes) {
            String requestStr = new String(requestBytes);
            Log.d(TAG, "onCharacteristicWriteRequest, device = " + device + "chara = " + characteristic.getUuid() + "requestId = " + requestId);
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, requestBytes);// ��Ӧ�ͻ���
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            Log.d(TAG, "onDescriptorReadRequest, device = " + device + ", requestId = " + requestId + "descriptor = " + descriptor.getUuid());
            String response = "ok";
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, response.getBytes()); // ��Ӧ�ͻ���
        }

        @Override
        public void onDescriptorWriteRequest(final BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            // ��ȡ�ͻ��˷�����������
            Log.d(TAG, "onDescriptorWriteRequest, device = " + device 
                    + ", requestId = " + requestId + "descriptor = " + descriptor.getUuid() + "offset = " + offset);
            String response = "ok";
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, response.getBytes());// ��Ӧ�ͻ���

            String requestValue = Arrays.toString(value);
            // ��ģ��֪ͨ�ͻ���Characteristic�仯
            if (descriptor.getUuid().toString().equals(DESC_NOTITY_UUID) && Arrays.toString(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE).equals(requestValue)) { //�Ƿ���֪ͨ
                final BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < 5; i++) {
                            SystemClock.sleep(2000);
                            Random rand = new Random();
                            mWeaterIndex = rand.nextInt(5);
                            Log.d(TAG, "onCharacteristicReadRequest, mWeaterIndex = " + mWeaterIndex);
                            mText.setText("New Weather: " + weather[mWeaterIndex]);
                            byte[] by = {(byte) mWeaterIndex};
                            characteristic.setValue(by);
                            mBluetoothGattServer.notifyCharacteristicChanged(device, characteristic, false);
                            Log.d(TAG,"notify Characteristic change, mWeaterIndex = " + mWeaterIndex);
                        }
                    }
                }).start();
            }
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            Log.d(TAG, "onExecuteWrite, device = " + device + ", requestId = " + requestId + ", execute = " + execute);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            Log.d(TAG, "onNotificationSent, device = " + device + ", status = " + status);
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            Log.d(TAG, "onMutChanged, device = " + device + "mtu = " + mtu);
        }
    };
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private  void requestPermissions(Context context) {
        if (Build.VERSION.SDK_INT < 23){return;}
//        //�ж��Ƿ���Ȩ��
//        PackageManager packageManager = context.getPackageManager();
//        if(PackageManager.PERMISSION_DENIED == 
//                packageManager.checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, "com.atlas.bleserver")) {
//                
//                PermissionInfo info;
//                packageManager.addPermission(info);
//        }
//        
//        if (getApplication().checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            //����Ȩ��
//           
//        }
//
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            //����Ȩ��
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
//        }
    }

}
