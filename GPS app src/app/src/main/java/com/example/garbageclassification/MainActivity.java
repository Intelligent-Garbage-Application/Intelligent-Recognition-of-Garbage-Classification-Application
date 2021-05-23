package com.example.garbageclassification;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    private final static int REQUEST_ENABLE_BT=1;
    private static final UUID MY_UUID=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String TAG = "Extract";
    InputStream inStream=null;
    private  static final String BTMAC="00:20:10:08:C0:4B";
    double defaultLa=27.918182;
    double defaultLo=120.653544;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MapView mapView=(MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        AMap aMap=mapView.getMap();


        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        BluetoothSocket socket=null;
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                if(BTMAC.equals(deviceHardwareAddress)){
                    try{socket=device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                    socket.connect();
                } catch (IOException e) {
                        Log.e(TAG, "connection failed\n");
                        if(socket!=null){
                            try {
                                socket.close();
                            } catch (IOException e1){
                                Log.e(TAG, "socket closure failed\n" );
                            }
                            }
                        }

                    try {
                        inStream =socket.getInputStream();
                    } catch(IOException e){
                        Log.e(TAG, "getInputStream failed\n" );
                    }
                    }
            }
        }
        StringBuffer GPSinfo = new StringBuffer();
        String line;
        String output=null;
        String message="test";
        String latitude="30";
        String longitude="120";
        BufferedReader br=new BufferedReader(new InputStreamReader(inStream));

        try{
            while ((line = br.readLine()) != null) {
                GPSinfo.append(line);
                output=GPSinfo.toString();
            }
        } catch(IOException e){
            Log.e(TAG, "Buffer read failed\n" );
        }
        if(output.contains("GPS")){
            message="GPS is not available\n";
        }
        else if(output.contains("latitude")){
            latitude=output.substring(11,19);
            longitude=output.substring(33,41);
            defaultLa=Double.valueOf(latitude)/100;
            defaultLo=Double.valueOf(longitude)/100;
        }


        LatLng trashcan1= new LatLng(defaultLa,defaultLo);
        LatLng center=new LatLng(27.918853,120.653000);
        aMap.moveCamera(CameraUpdateFactory.changeLatLng(center));
        aMap.moveCamera(CameraUpdateFactory.zoomTo(18));
        BitmapDescriptor bitmap=BitmapDescriptorFactory.fromResource(R.mipmap.trashcan_small);
        //View markerView= LayoutInflater.from(this).inflate(R.layout.activity_main,mapView,false);
        Marker binMarker1 =aMap.addMarker(new MarkerOptions().position(trashcan1).title("TrashBin").icon(bitmap));
        TextView tv=(TextView)findViewById(R.id.tv);
        tv.setText(message);


    }

}