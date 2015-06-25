package com.matsumo.ble_notification_sample;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.UUID;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String DEVICE_ADDRESS = "B4:99:4C:4A:E7:84";   //TODO set your mac address

    private static final UUID BAND_SERVICE_PHONE_ALERT_UUID = UUID.fromString("f000ff10-0451-4000-b000-000000000000");
    private static final UUID BAND_CHARACTERISTIC_PHONE_ALERT_UUID = UUID.fromString("f000ff11-0451-4000-b000-000000000000");

    private BluetoothGatt mBluetoothGatt = null;
    private BluetoothDevice mDevice;
    private Button btnSend;
    private EditText etText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnSend = (Button) findViewById(R.id.button);
        etText = (EditText) findViewById(R.id.editText);
        btnSend.setEnabled(false);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = etText.getText().toString();
                if (!TextUtils.isEmpty(text)) sendNotification(text);
            }
        });

        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBtAdapter = mBluetoothManager.getAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetoothアダプターが見つかりません", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!mBtAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetoothを有効にしてください", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        mDevice = mBtAdapter.getRemoteDevice(DEVICE_ADDRESS);
        mBluetoothGatt = mDevice.connectGatt(this, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
                        mBluetoothGatt.discoverServices();
                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        mBluetoothGatt = null;
                        btnSend.post(new Runnable() {
                            @Override
                            public void run() {
                                btnSend.setEnabled(false);
                            }
                        });
                        break;
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    btnSend.post(new Runnable() {
                        @Override
                        public void run() {
                            btnSend.setEnabled(true);
                        }
                    });
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt = null;
        }
    }

    private void sendNotification(String text) {
        byte[][] r = makeBitmapPacket(text);
        for (int i = 0; i < r.length; i++) {
            BluetoothGattCharacteristic c = characteristic(BAND_SERVICE_PHONE_ALERT_UUID, BAND_CHARACTERISTIC_PHONE_ALERT_UUID);
            c.setValue(r[i]);
            mBluetoothGatt.writeCharacteristic(c);
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
            }
        }
        BluetoothGattCharacteristic c = characteristic(BAND_SERVICE_PHONE_ALERT_UUID, BAND_CHARACTERISTIC_PHONE_ALERT_UUID);
        c.setValue(new byte[]{2/*icon:phone=1, sms=2*/, (byte) 192/*bitmap size*/, 1, 16});
        mBluetoothGatt.writeCharacteristic(c);
    }

    private BluetoothGattCharacteristic characteristic(UUID sid, UUID cid) {
        if (mBluetoothGatt == null) return null;
        BluetoothGattService s = mBluetoothGatt.getService(sid);
        if (s == null) {
            Log.w(TAG, "Service NOT found :" + sid.toString());
            return null;
        }
        BluetoothGattCharacteristic c = s.getCharacteristic(cid);
        if (c == null) {
            Log.w(TAG, "Characteristic NOT found :" + cid.toString());
            return null;
        }
        return c;
    }

    private byte[][] makeBitmapPacket(String c) {
        int len = 6;
        byte size = (byte) (len * 16 * 2);

        Paint w_paint = new Paint();
        w_paint.setAntiAlias(false);
        w_paint.setColor(Color.BLACK);
        w_paint.setTextSize(14f);
        w_paint.setTextScaleX(10f / 14f);
        Paint.FontMetrics fm = w_paint.getFontMetrics();
        Bitmap bmp = Bitmap.createBitmap(16 * len, 16, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        cv.drawText(c, 0, 8 - (fm.ascent + fm.descent) / 2, w_paint);

        byte[][] r = new byte[len * 2][4 + 32];
        for (int d = 0; d < len * 2; d++) {
            for (int y = 0; y < 8; y++) {
                int d1 = 0, d2 = 0;
                for (int x = 0; x < 8; x++) {
                    d1 |= bmp.getPixel((d / 2) * 16 + x, (d & 1) * 8 + y) == Color.BLACK ? 1 : 0;
                    d2 |= bmp.getPixel((d / 2) * 16 + x + 8, (d & 1) * 8 + y) == Color.BLACK ? 1 : 0;
                    if (x < 7) {
                        d1 <<= 1;
                        d2 <<= 1;
                    }
                }
                r[d][4 + y * 2] = (byte) d1;
                r[d][4 + y * 2 + 1] = (byte) d2;
            }
            r[d][0] = 2;
            r[d][1] = size;
            r[d][2] = 1;
            r[d][3] = (byte) (d + 1);
        }
        bmp.recycle();
        return r;
    }
}
