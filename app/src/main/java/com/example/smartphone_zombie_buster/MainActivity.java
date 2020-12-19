package com.example.smartphone_zombie_buster;
/*参考： https://akira-watson.com/android/gps-background.html, https://iyemon018.hatenablog.com/entry/2017/12/28/012030*/
import androidx.appcompat.app.AppCompatActivity;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import com.google.android.gms.maps.GoogleMap;

import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;


public class MainActivity extends AppCompatActivity implements WakeLockListener, OnMapReadyCallback {

    private WakeLockBroadcastReceiver wakeLockBroadcastReceiver;

    private StorageReadWrite fileReadWrite;
    private static final int REQUEST_MULTI_PERMISSIONS = 101;
    private TextView textView;
    private GoogleMap mMap;
    private MapView mMapView;
    private int drawId = -1;
    double[] lastPos = {0,0};
    private Polyline line;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Context context = getApplicationContext();
        fileReadWrite = new StorageReadWrite(context);

        textView = findViewById(R.id.log_text);
        textView.setText("本日のポイント： "+fileReadWrite.getDistance()+" pt");

        wakeLockBroadcastReceiver = new WakeLockBroadcastReceiver(this);
        registerReceiver(wakeLockBroadcastReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
        registerReceiver(wakeLockBroadcastReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));

        // Android 6, API 23以上でパーミッシンの確認
        if(Build.VERSION.SDK_INT >= 23){
            checkMultiPermissions();
        }

        Button buttonReset = findViewById(R.id.button_reset);
        buttonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Serviceの停止
                fileReadWrite.clearFile();
                drawId = -1;
                textView.setText("本日のポイント： "+fileReadWrite.getDistance()+" pt");
                mMap.clear();
            }
        });

        // MapFragmentの生成
        MapFragment mapFragment = MapFragment.newInstance();

        // MapViewをMapFragmentに変更する
        FragmentTransaction fragmentTransaction =
                getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.mapView, mapFragment);
        fragmentTransaction.commit();

        mapFragment.getMapAsync(this);

        mMapView = (MapView) findViewById(R.id.mapView);
    }

    @Override
    public void onScreenOn() {
        Intent intent = new Intent(getApplication(), LocationService.class);
        stopService(intent);
        textView.setText("本日のポイント： "+fileReadWrite.getDistance()+" pt");
    }

    @Override
    public void onScreenOff() {
        Intent intent = new Intent(getApplication(), LocationService.class);
        // API 26 以降
        startForegroundService(intent);
        drawRout();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.setMyLocationEnabled(true);
        drawRout();
    }

    void drawRout(){
        List<double[]> pos = fileReadWrite.getPositions();
        if(pos!=null)
        {
            for(double[] entry : pos){
                int id = (int) entry[0];
                if(id > drawId)
                {
                    drawId = id;
                    lastPos[0] = entry[1];
                    lastPos[1] = entry[2];
                }
                else if(id == drawId)
                {
                    double[] currentPos = {entry[1], entry[2]};
                    LatLng lastLng = new LatLng(lastPos[0], lastPos[1]);
                    LatLng currentLng = new LatLng(currentPos[0], currentPos[1]);
                    PolylineOptions straight = new PolylineOptions().
                            add(lastLng, currentLng)
                            .geodesic(false)
                            .color(Color.RED)
                            .width(3);
                    line = mMap.addPolyline(straight);
                    lastPos[0] = currentPos[0];
                    lastPos[1] = currentPos[1];
                }
            }
        }
    }

    // 位置情報許可の確認、外部ストレージのPermissionにも対応できるようにしておく
    private  void checkMultiPermissions(){
        // 位置情報の Permission
        int permissionLocation = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        // 外部ストレージ書き込みの Permission
        int permissionExtStorage = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        ArrayList reqPermissions = new ArrayList<>();

        // 位置情報の Permission が許可されているか確認
        if (permissionLocation == PackageManager.PERMISSION_GRANTED) {
            // 許可済
        }
        else{
            // 未許可
            reqPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // 外部ストレージ書き込みが許可されているか確認
        if (permissionExtStorage == PackageManager.PERMISSION_GRANTED) {
            // 許可済
        }
        else{
            // 許可をリクエスト
            reqPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        // 未許可
        if (!reqPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    (String[]) reqPermissions.toArray(new String[0]),
                    REQUEST_MULTI_PERMISSIONS);
            // 未許可あり
        }
    }

    // 結果の受け取り
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_MULTI_PERMISSIONS) {
            if (grantResults.length > 0) {
                for (int i = 0; i < permissions.length; i++) {
                    // 位置情報
                    if (permissions[i].
                            equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            // 許可された

                        } else {
                            // それでも拒否された時の対応
                            toastMake("位置情報の許可がないので計測できません");
                        }
                    }
                    // 位置情報
                    else if (permissions[i].
                            equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            // 許可された

                        } else {
                            // それでも拒否された時の対応
                            toastMake("位置情報の許可がないので計測できません");
                        }
                    }
                    // 位置情報
                    else if (permissions[i].
                            equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            // 許可された

                        } else {
                            // それでも拒否された時の対応
                            toastMake("位置情報の許可がないので計測できません");
                        }
                    }
                    // 外部ストレージ
                    else if (permissions[i].
                            equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            // 許可された
                        } else {
                            // それでも拒否された時の対応
                            toastMake("外部書込の許可がないので書き込みできません");
                        }
                    }
                }
            }
        }
        else{
            //
        }
    }

    // トーストの生成
    private void toastMake(String message){
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        // 位置調整
        toast.setGravity(Gravity.CENTER, 0, 200);
        toast.show();
    }
}