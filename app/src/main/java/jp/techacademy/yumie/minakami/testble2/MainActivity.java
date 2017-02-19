package jp.techacademy.yumie.minakami.testble2;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.neovisionaries.bluetooth.ble.advertising.ADPayloadParser;
import com.neovisionaries.bluetooth.ble.advertising.ADStructure;
import com.neovisionaries.bluetooth.ble.advertising.EddystoneEID;
import com.neovisionaries.bluetooth.ble.advertising.EddystoneTLM;
import com.neovisionaries.bluetooth.ble.advertising.EddystoneUID;
import com.neovisionaries.bluetooth.ble.advertising.EddystoneURL;
import com.neovisionaries.bluetooth.ble.advertising.IBeacon;

import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothAdapter.LeScanCallback mLeScanCallback;    // for API 19 or former
//    private LeDeviceListAdapter mLeDeviceListAdapter;

    private Handler mHandler;

    private static final int    BT_REQUEST_ENABLE = 1;
    private static final long   SCAN_PERIOD = 10000;

    TextView mTextMajor;
    TextView mTextMinor;
    TextView mTextPower;
    TextView mTextuid;
    TextView mTextRssi;

    UUID mUuid;
    int mMajor;
    int mMinor;
    int mPower;
    int mRssi;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();

        Log.d("life", "onCreate");

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // initialize Bluetooth Adapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // check Bluetooth supported
        if(mBluetoothAdapter == null){
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if(Build.VERSION.SDK_INT == 18 || Build.VERSION.SDK_INT == 19){
            // if API 18-19
            mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] scanRecord) {
                    scanData(bluetoothDevice, i, scanRecord);
                }
            };
        } else {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mTextMajor = (TextView) findViewById(R.id.major);
        mTextMinor = (TextView) findViewById(R.id.minor);
        mTextPower = (TextView) findViewById(R.id.txpower);
        mTextuid   = (TextView) findViewById(R.id.uuid);
        mTextRssi = (TextView) findViewById(R.id.rssi);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d("life", "onActivityResult");

        // User chose not to enable Bluetooth.
        if (requestCode == BT_REQUEST_ENABLE && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d("life", "onResume");

        // check Bluetooth supported
        if(mBluetoothAdapter == null){
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Ensures Bluetooth is enabled on the device.
        // If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d("life", "R.string.bt_off @ onResume");

            Toast.makeText(this, R.string.bt_on_request, Toast.LENGTH_SHORT).show();
            Intent setBtIntnt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(setBtIntnt, BT_REQUEST_ENABLE);
        }
        startBleScan();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopBleScan();
        Log.d("life", "onPause");
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onDestroy(){
        stopBleScan();

        super.onDestroy();
    }

    @SuppressWarnings("deprecation")
    protected void startBleScan(){
        if(Build.VERSION.SDK_INT == 18 || Build.VERSION.SDK_INT == 19){
            // API is 18-19
            // scan stops after SCAN_PERIOD msec
            if(mBluetoothAdapter != null){

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBluetoothAdapter.startLeScan(mLeScanCallback);

                        // INSERT : BLE STATUS CHECK
                    }
                }, SCAN_PERIOD);

                mBluetoothAdapter.startLeScan(mLeScanCallback);
            }

            Log.d("life", "startBleScan, kitkat <");

        } else {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();

            Log.d("life", "startBleScan,  except kitkat");

            finish();
            return;
        }
    }

    @SuppressWarnings("deprecation")
    protected void stopBleScan(){
        if(Build.VERSION.SDK_INT == 18 || Build.VERSION.SDK_INT == 19){
            // API is 18-19
            if(mBluetoothAdapter != null){
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }

            Log.d("life", "stopBleScan, kitkat <");

        } else {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();

            Log.d("life", "stopBleScan, except kitkat");

            finish();
            return;
        }
    }

    @SuppressWarnings("deprecation")
    protected void scanData(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord){

        boolean flag = false;

        // Parse the payload of the advertising packet.
        List<ADStructure> str = ADPayloadParser.getInstance().parse(scanRecord);

        for(ADStructure structure : str){
            // if the ADStructure instance can be cast to IBeacon
            if(structure instanceof IBeacon){

                flag = true;

                IBeacon iBeacon = (IBeacon) structure;

                mUuid = iBeacon.getUUID();  // Proximity UUID
                mMajor = iBeacon.getMajor(); // Major number
                mMinor = iBeacon.getMinor(); // Minor number
                mPower = iBeacon.getPower(); // tx power
                mRssi = rssi;
                Log.d("life", "IBeacon");
                Log.d("life", "uuid : " + mUuid + ", major : " + mMajor + ", minor : " + mMinor + ", power : " + mPower + ", rssi : " + mRssi);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mTextuid.setText(mUuid.toString());
                        mTextMajor.setText(String.valueOf(mMajor));
                        mTextMinor.setText(String.valueOf(mMinor));
                        mTextPower.setText(String.valueOf(mPower));
                        mTextRssi.setText(String.valueOf(mRssi));
                    }
                });
            } else if(structure instanceof EddystoneUID){
                // do nothing
                flag = true;
                Log.d("life", "EddystoneUID");
            } else if(structure instanceof EddystoneURL){
                // do nothing
                flag = true;
                Log.d("life", "EddystoneURL");
            } else if(structure instanceof EddystoneTLM){
                //
                flag = true;
                Log.d("life", "EddystoneTLM");
            } else if(structure instanceof EddystoneEID){
                //
                flag = true;
                Log.d("life", "EddystoneEID");
            }
        }

        if(Build.VERSION.SDK_INT == 18 || Build.VERSION.SDK_INT == 19) {
            if (flag == true && mBluetoothAdapter != null) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }
    }
}
