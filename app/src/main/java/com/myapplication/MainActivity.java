package com.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import com.skt.tmap.engine.navigation.SDKManager;
import com.skt.tmap.vsm.data.VSMMapPoint;
import com.skt.tmap.vsm.map.marker.VSMMarkerBase;
import com.skt.tmap.vsm.map.marker.VSMMarkerManager;
import com.tmapmobility.tmap.tmapsdk.ui.fragment.NavigationFragment;
import com.tmapmobility.tmap.tmapsdk.ui.util.TmapUISDK;
import com.skt.tmap.vsm.map.marker.VSMMarkerCircle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import android.location.LocationManager;
import android.location.LocationListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "TMapUISDKSample";

    private static String CLIENT_ID = "";
    private static String API_KEY = "l7xx0bc4bad137d845b4927a3d73f7c58cf7"; //발급받은 KEY

    private NavigationFragment navigationFragment;
    private FragmentManager fragmentManager;
    private FragmentTransaction transaction;

    private Button button1, button2, button3, button4, debugbutton;
    private LinearLayout buttonLayout, infoLayout, debugLayout;

    private List<String[]> csv;
    private VSMMarkerManager vsmMarkerManager;
    private LocationManager locationManager;
    private List<String[]> matched = new ArrayList<>();
    private List<String[]> coordinates = new ArrayList<>();
    private String log = "";
    private TextView textView, textView2;
    private FrameLayout mainLayout;

    private TextView gpsText;
    private TextView sdkText;
    private TextView greenCount;
    private TextView speedText;
    private TextView gpslostText;
    private List<Integer> colorarr = new ArrayList<>();
    private Location lastLocation;
    private boolean GPSON;
    private int lastcircleid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();
        gpsText = (TextView) findViewById(R.id.GPSTEXT);
        sdkText = (TextView) findViewById(R.id.SDKTEXT);
        greenCount = (TextView) findViewById(R.id.greenCount);
        speedText = (TextView) findViewById(R.id.speedtext);
        gpslostText = (TextView) findViewById(R.id.gpsLosttext);

        colorarr = new ArrayList<>();
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        GPSON = false;
        Handler mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }
        }, 10000);

//        Timer timer = new Timer();
//
//        TimerTask TT = new TimerTask() {
//            @Override
//            public void run() {
//                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
//                    Log.e(TAG, "ProviderEnabled");
//                } else {
//                    Log.e(TAG, "ProviderDisabled");
//                }
//            }
//        };
//        timer.schedule(TT, 1000, 1000);

    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            Location sdkLocation = SDKManager.getInstance().getCurrentPosition();
            String provider = location.getProvider();  // 위치정보
            double longitude = location.getLongitude(); // 위도
            double latitude = location.getLatitude(); // 경도
            double altitude = location.getAltitude(); // 고도
            gpsText.setText("위치정보(Android) : " + provider + "\n" + "위도 : " + longitude + "\n" + "경도 : " + latitude + "\n" + "고도 : " + altitude);
            String provider2 = sdkLocation.getProvider();
            double longitude2 = sdkLocation.getLongitude();
            double latitude2 = sdkLocation.getLatitude();
            double altitude2 = sdkLocation.getAltitude();
            sdkText.setText("위치정보(TMAP) : " + provider2 + "\n" + "위도 : " + longitude2 + "\n" + "경도 : " + latitude2 + "\n" + "고도  : " + altitude2);
            int count = 0;
            if (colorarr != null) {
                count = Collections.frequency(colorarr, 1);
                greenCount.setText("GPS COUNT : " + count);
            }
            double speed = location.getSpeed();
            double speed2 = sdkLocation.getSpeed();
            speedText.setText("속도(m/s) : " + "\n" + speed + "\n" + "속도(km/h) : " + "\n" + speed * 3.6 + "\n" + "Tmap속도(m/s) : " + "\n" + speed2 + "\n" + "Tmap속도(km/h) : " + "\n" + speed2 * 3.6);
            Log.d(TAG, "matching functioning");
            matching(location);
            if (lastLocation.getLatitude() == location.getLatitude() && lastLocation.getLongitude() == location.getLongitude()){
                Log.e(TAG, "GPS lost");
                GPSON = false;
                gpsLost(location);
                gpslostText.setText("GPS LOST\n" + "위성 개수 : " + location.getExtras().getInt("satellites"));
            } else {
                Log.e(TAG, "GPS ON");
                lastLocation = location;
                GPSON = true;
                gpslostText.setText("GPS ON\n" + "위성 개수 : " + location.getExtras().getInt("satellites"));
            }
        }

        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled");

        }

        @Override

        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled");
        }
    };

    public void gpsLost(Location location) {
            int id = lastcircleid;
            Log.e(TAG, "lastIndex : " + String.valueOf(id));
            if (id == -1) {
                id = 0;
            }
            double speed = location.getSpeed();
            int gap = 10;
            changeColor2(id,speed,gap);
    }

//    private int matching2(Location location) {
//        int i = 0;
//        for (String[] grid : csv) {
//            if (inGrid(location, grid)) {
//                return i;
//            }
//        }
//        return 0;
//    }

    private void changeColor2(int id, double speed, int gap) {
        Timer timer = new Timer();

        TimerTask TT = new TimerTask() {
            @Override
            public void run() {
                for (int i = id + 1; i < colorarr.size(); i++) {
                    if (GPSON) {
                        break;
                    }
                    colorarr.set(i,1);
                    String markId = "m" + i;
                    VSMMarkerBase vsmMarkerBase = vsmMarkerManager.getMarker(markId);
                    Log.e(TAG, String.valueOf(markId));
                    ((VSMMarkerCircle) vsmMarkerBase).setFillColor(1900000000);
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        timer.schedule(TT, 1000);
    }


    private void checkPermission() {

        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            initUI();
            initUISDK();
        } else {
            String[] permissionArr = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
            requestPermissions(permissionArr, 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            initUI();
            initUISDK();
        } else {
            Toast.makeText(this, "위치 권한이 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initUI() {
        fragmentManager = getSupportFragmentManager();

        navigationFragment = TmapUISDK.Companion.getFragment();

        transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.tmapUILayout, navigationFragment);
        transaction.commitAllowingStateLoss();

        buttonLayout = findViewById(R.id.buttonLayout);
        debugLayout = findViewById(R.id.debugLayout);

        textView = findViewById(R.id.textView);
//      textView2 = findViewById(R.id.textView2);

        mainLayout = findViewById(R.id.tmapUILayout);
        infoLayout = findViewById(R.id.tmapINFOLayout);
        infoLayout.setVisibility(View.GONE);

        button1 = findViewById(R.id.button1);
        button2 = findViewById(R.id.button2);
        button3 = findViewById(R.id.button3);
        button4 = findViewById(R.id.button4);
        debugbutton = findViewById(R.id.debugButton);

        button1.setOnClickListener(onClickButton);
        button2.setOnClickListener(onClickButton);
        button3.setOnClickListener(onClickButton);
        button4.setOnClickListener(onClickButton);
        debugbutton.setOnClickListener(onCLickDebugButton);

        //네비게이션 상태 변경 시 callback
        navigationFragment.setDrivingStatusCallback(new TmapUISDK.DrivingStatusCallback() {
            @Override
            public void onStartNavigation() {
                //네비게이션 시작
            }

            @Override
            public void onStopNavigation() {
                //네비게이션 종료
                buttonLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPermissionDenied(int i, @Nullable String s) {
                //권한 없을 때
            }
        });


    }

    private final View.OnClickListener onClickButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (v.equals(button1)) {
                clickButton1();
            } else if (v.equals(button2)) {
                clickButton2();
            } else if (v.equals(button3)) {
                clickButton3();
            }
            else if (v.equals(button4)) {
                clickButton4();
            }
        }
    };

    private final View.OnClickListener onCLickDebugButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (debugLayout.getVisibility() == View.GONE) {
                debugLayout.setVisibility(View.VISIBLE);
            } else {
                debugLayout.setVisibility(View.GONE);
            }
        }
    };
    /**
     * 안전운행
     */
    private void clickButton1() {
        buttonLayout.setVisibility(View.GONE);
        navigationFragment.startSafeDrive();
    }

    /**
     * 경로안내
     */
    private void clickButton2() {
        mainLayout.setVisibility(View.GONE);
        buttonLayout.setVisibility(View.GONE);
        infoLayout.setVisibility(View.VISIBLE);
    }

    /**
     * 경유지 추가 경로안내
     */
    private void clickButton3() {}

    private void clickButton4() {
        String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName ="out.csv";
        String filePath = baseDir + File.separator + "Download" + File.separator + fileName;

        CSVWriter csvWriter = null;
        Log.d(TAG, filePath);
        try {
            csvWriter = new CSVWriter(new FileWriter(filePath));
            csvWriter.writeAll(coordinates);
            csvWriter.close();
            Log.d(TAG, "Successfully saved");
        } catch (IOException e) {
            System.out.println(e.getMessage());
            Log.e(TAG, "Failed to save");
        }


    }
    private void initUISDK() {
        TmapUISDK.Companion.initialize(this, CLIENT_ID, API_KEY, new TmapUISDK.InitializeListener() {
            @Override
            public void onSuccess() {
                try {
                    loadData();
                } catch (IOException e) {
                    throw new RuntimeException("IO Error");
                } catch (CsvException e) {
                    throw new RuntimeException("CSV Error");
                }
                Log.e(TAG, "success initialize");
            }

            @Override
            public void onFail(int i, @Nullable String s) {
                Log.e(TAG, "fail initialize : " + s);
            }
        });
    }

    private void loadData() throws IOException, CsvException {
        AssetManager assetManager = this.getAssets();
        InputStream inputStream = assetManager.open("namsan.csv");
        CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream));

        vsmMarkerManager = TmapUISDK.Companion.getFragment().b.mapEngine().getMarkerManager();

        List<String[]> allContent = (List<String[]>) csvReader.readAll();
        csv = allContent;
        colorarr = new ArrayList<Integer>();

        for (String content[] : allContent) {
            VSMMarkerCircle vsmMarkerCircle = ((VSMMarkerCircle.Builder) ((VSMMarkerCircle.Builder) ((VSMMarkerCircle.Builder) ((VSMMarkerCircle.Builder) (new VSMMarkerCircle.Builder(content[0])).renderOrder(VSMMarkerBase.RENDERING_ORDER_AFTER_POINT_MARKER)).showPriority(50.0F)).visible(true)).touchable(false)).position(new VSMMapPoint(Double.parseDouble(content[1]), Double.parseDouble(content[2]))).fillColor(269971430).strokeColor(1712812006).strokeWidth(1.0F).radius(5.0F).visibleRadius(false).create();
            vsmMarkerManager.addMarker((VSMMarkerBase) vsmMarkerCircle);
            colorarr.add(0);
        }
    }

    private void changeColor(String[] grid) {
        VSMMarkerBase vsmMarkerBase = vsmMarkerManager.getMarker(grid[0]);
        lastcircleid = Integer.parseInt(grid[0].substring(1,grid[0].length()));
        ((VSMMarkerCircle) vsmMarkerBase).setFillColor(1900000000);
//        Timer timer = new Timer();
//
//        TimerTask TT = new TimerTask() {
//            @Override
//            public void run() {
//                if (!inGrid(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER), grid)) {
//                    ((VSMMarkerCircle) vsmMarkerBase).setFillColor(269971430);
//                    timer.cancel();
//                }
//            }
//        };
//        timer.schedule(TT, 10000, 1000);
    }

    private void matching(Location location) {
        boolean check = true;
        log = "\n  " + location.getLatitude() + " " + location.getLongitude();
        coordinates.add(new String[]{String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude())});
        int i = 0;
        for (String[] grid : csv) {
            if (inGrid(location, grid)) {
                changeColor(grid);
                textView.append(log);
                colorarr.set(i,1);
                return;
            }
            i++;
        }
        Spannable spannable = new SpannableString(log);
        ForegroundColorSpan fcsRed = new ForegroundColorSpan(Color.RED);
        spannable.setSpan(fcsRed, 0, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.append(spannable);
    }

    private boolean inGrid(Location location, String[] grid) {
        if (location == null) {
            return false;
        }
        Double longi = location.getLongitude();
        Double lati = location.getLatitude();
        Boolean isIn = false;

        if (longi >= Double.parseDouble(grid[3]) && longi <= Double.parseDouble(grid[4]) && lati >= Double.parseDouble(grid[5]) && lati <= Double.parseDouble(grid[6])) {
            isIn = true;
            if (!matched.contains(grid)) {
                matched.add(grid);
//                textView2.setText(matched.size() + "/126");

            }
        }

        return isIn;
    }

    @Override
    public void onBackPressed() {
        if (!navigationFragment.onBackKeyPressed()) {
            buttonLayout.setVisibility(View.VISIBLE);
            infoLayout.setVisibility(View.GONE);
            mainLayout.setVisibility(View.VISIBLE);
        }
    }
}