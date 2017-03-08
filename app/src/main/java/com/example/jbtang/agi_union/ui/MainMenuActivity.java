package com.example.jbtang.agi_union.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.graphics.Color;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jbtang.agi_union.R;
import com.example.jbtang.agi_union.core.Global;
import com.example.jbtang.agi_union.core.Status;
import com.example.jbtang.agi_union.dao.configuration.ConfigurationDAO;
import com.example.jbtang.agi_union.dao.configuration.ConfigurationDBManager;
import com.example.jbtang.agi_union.dao.devices.DeviceDAO;
import com.example.jbtang.agi_union.dao.devices.DeviceDBManager;
import com.example.jbtang.agi_union.dao.logInfos.LogInfoManager;
import com.example.jbtang.agi_union.device.DeviceManager;
import com.example.jbtang.agi_union.device.MonitorDevice;
import com.example.jbtang.agi_union.external.MonitorApplication;
import com.example.jbtang.agi_union.external.MonitorHelper;
import com.example.jbtang.agi_union.external.service.MonitorService;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Created by xiang on 2016/1/11.
 */
public class MainMenuActivity extends AppCompatActivity {
    private static final int DEFAULT_TRIGGER_INTERVAL_SMS_MAX = 30;//触发间隔
    private static final int DEFAULT_SMS_FILTER_INTERVAL_MAX = 4;//短信过滤间隔
    private static final int DEFAULT_SILENCECHECKTIME = 20;
    private static final int DEFAULT_RECEIVINGANTENNANUM = 2;
    private static final int DEFAULT_TOTAL_TRIGGER_COUNT = 30;//总触发数
    private static final String SAVE_SUCCEED = "保存成功";
    private static final String WRONG_PHONE_NUM = "号码输入有误";
    private static final Pattern PHONE_NUMBER = Pattern.compile("^((13[0-9])|(15[^4,\\D])|(18[0-9]))\\d{8}$");
    private GridView gridView;
    private ArrayList<HashMap<String, Object>> itemList;
    private SimpleAdapter simpleAdapter;
    private String texts[];
    private int images[];
    private long exitTime = 0;
    private RadioGroup modelChoose;
    private EditText phoneNum;
    private Button saveBtn;
    private Button rebootBtn;
    private Button connectBtn;
    private Button disconnectBtn;
    private TextView deviceStatusText;
    private TextView deviceColorOne;
    private TextView deviceColorTwo;
    private TextView deviceColorThree;
    private TextView deviceColorFour;
    private DeviceDBManager dmgr;
    private ConfigurationDBManager cmgr;
    private ConfigurationDAO dao;
    private myBroadcastReceiver broadcastReceiver;
    private MonitorHelper monitorHelper;
    private LocationManager locationManager;
    private String locationProvider;
    private LogInfoManager logInfoManager;
    private boolean stopConnecting;
    private SharedPreferences agiUnionSP;
    private static final String MAINMENU_DERECTORY = "main_menu";
    private static final String TARGET_PHONE = "target_phone";
    private static final String MODEL = "model";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        setContentView(R.layout.activity_main_menu);
        init();
        logInfoManager = new LogInfoManager(this);
        Global.LogInfo.startTime = new Date().toString();
        setLocation();
    }

    public void init() {
        images = new int[]{R.drawable.cell_monitor,
                R.drawable.find_stmsi,
                R.drawable.orientation,
                R.drawable.interference,
                R.drawable.configuration,
                R.drawable.local_info};
        texts = new String[]{this.getString(R.string.title_main_menu_cell_monitor),
                this.getString(R.string.title_main_menu_find_STMSI),
                this.getString(R.string.title_main_menu_orientation),
                this.getString(R.string.title_main_menu_environment_detection),
                this.getString(R.string.title_main_menu_configuration),
                this.getString(R.string.title_main_menu_cellPhone_info)};
        itemList = new ArrayList<HashMap<String, Object>>();
        for (int i = 0; i < 6; i++) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("itemImage", images[i]);
            map.put("itemText", texts[i]);
            itemList.add(map);
        }
        simpleAdapter = new SimpleAdapter(this,
                itemList,
                R.layout.main_menu_item,
                new String[]{"itemImage", "itemText"},
                new int[]{R.id.main_menu_item_image, R.id.main_menu_item_name});
        gridView = (GridView) findViewById(R.id.main_menu_gridView);
        gridView.setAdapter(simpleAdapter);
        gridView.setOnItemClickListener(new ItemClickListener());

        modelChoose = (RadioGroup) findViewById(R.id.main_menu_model_choose);
        phoneNum = (EditText) findViewById(R.id.main_menu_target_phone_num);
        saveBtn = (Button) findViewById(R.id.main_menu_target_phone_num_save);

        rebootBtn = (Button) findViewById(R.id.main_menu_device_reboot);
        connectBtn = (Button) findViewById(R.id.main_menu_device_connnect);
        disconnectBtn = (Button) findViewById(R.id.main_menu_device_disconnect);
        rebootBtn.setOnClickListener(new rebootBtnOnClickListener());
        connectBtn.setOnClickListener(new conBtnOnClickListener());
        disconnectBtn.setOnClickListener(new disconBtnOnClickListener());

        deviceColorOne = (TextView) findViewById(R.id.main_menu_device_background_one);
        deviceColorTwo = (TextView) findViewById(R.id.main_menu_device_background_two);
        deviceColorThree = (TextView) findViewById(R.id.main_menu_device_background_three);
        deviceColorFour = (TextView) findViewById(R.id.main_menu_device_background_four);
        deviceStatusText = (TextView) findViewById(R.id.main_menu_device_status_text);

        dmgr = new DeviceDBManager(this);
        getDevices();

        cmgr = new ConfigurationDBManager(this);
        dao = cmgr.getConfiguration(Global.UserInfo.user_name);
        saveToCache();
        saveToDAO();

        agiUnionSP = getSharedPreferences(MAINMENU_DERECTORY, Context.MODE_PRIVATE);
        Global.Configuration.targetPhoneNum = agiUnionSP.getString(TARGET_PHONE, "");
        phoneNum.setText(Global.Configuration.targetPhoneNum);
        Global.Configuration.model = agiUnionSP.getString(MODEL, "").equals("carry") ? Status.Model.CARRY : Status.Model.VEHICLE;
        modelChoose.check(Global.Configuration.model == Status.Model.CARRY ? R.id.main_menu_model_carry : R.id.main_menu_model_vehicle);
        modelChoose.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Global.Configuration.model = checkedId == R.id.main_menu_model_carry ? Status.Model.CARRY : Status.Model.VEHICLE;
            }
        });
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (phoneNum.getText().toString().toCharArray().length == 11) {
                    Global.Configuration.targetPhoneNum = phoneNum.getText().toString();
                    saveToDAO();
                    SharedPreferences.Editor agiUnionEditor = agiUnionSP.edit();
                    agiUnionEditor.putString(TARGET_PHONE, Global.Configuration.targetPhoneNum);
                    if(Global.Configuration.model == Status.Model.CARRY)
                        agiUnionEditor.putString(MODEL, "carry");
                    else
                        agiUnionEditor.putString(MODEL, "vehicle");
                    agiUnionEditor.commit();
                    Toast.makeText(getApplicationContext(), SAVE_SUCCEED, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), WRONG_PHONE_NUM, Toast.LENGTH_SHORT).show();
                }
            }
        });
        try {
            broadcastReceiver = new myBroadcastReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(MonitorApplication.BROAD_FROM_MAIN_MENU_DEVICE);
            filter.addAction(MonitorApplication.BROAD_FROM_CONFIGURATION_ACTIVITY);
            registerReceiver(broadcastReceiver, filter);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Global.ThreadPool.scheduledThreadPool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                refreshDeviceStatusBar();
            }
        }, 1, 1, TimeUnit.SECONDS);
        monitorHelper = new MonitorHelper();
        monitorHelper.startService(this);
    }

    private void setLocation() {
        //获取地理位置管理器
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //获取所有可用的位置提供器
        List<String> providers = locationManager.getProviders(true);
        if (providers.contains(LocationManager.GPS_PROVIDER)) {
            //如果是GPS
            locationProvider = LocationManager.GPS_PROVIDER;
        } else if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
            //如果是Network
            locationProvider = LocationManager.NETWORK_PROVIDER;
        } else if (providers.contains(LocationManager.PASSIVE_PROVIDER)) {
            locationProvider = LocationManager.PASSIVE_PROVIDER;
        } else {
            Toast.makeText(this, "没有可用的位置提供器", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.e(TAG, "locationProvider:" + locationProvider);
        //获取Location
        locationManager.requestLocationUpdates(locationProvider, 1000, 0, locationListener);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location location = locationManager.getLastKnownLocation(locationProvider);
        if (location != null) {
            Global.LogInfo.longitude = location.getLongitude() + "";
            Global.LogInfo.latitude = location.getLatitude() + "";
        }
        Toast.makeText(this, "经度：" + Global.LogInfo.longitude + "纬度：" + Global.LogInfo.latitude, Toast.LENGTH_SHORT).show();
        Log.e(TAG, "Global.LogInfo.longitude" + Global.LogInfo.longitude + ",Global.LogInfo.latitude" + Global.LogInfo.latitude);

    }

    LocationListener locationListener = new LocationListener() {

        @Override
        public void onStatusChanged(String provider, int status, Bundle arg2) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }

        @Override
        public void onLocationChanged(Location location) {
            //如果位置发生变化,重新显示
            Global.LogInfo.longitude = location.getLongitude() + "";
            Global.LogInfo.latitude = location.getLatitude() + "";
            Toast.makeText(getApplicationContext(), "经度：" + Global.LogInfo.longitude + "纬度：" + Global.LogInfo.latitude, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Global.LogInfo.longitude" + Global.LogInfo.longitude + ",Global.LogInfo.latitude" + Global.LogInfo.latitude);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private static final String TAG = "DeviceConfiguration";
    private static final String IP_SPLITTER = ".";

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void exit() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Toast.makeText(this, "再按一次退出程序",
                    Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            SharedPreferences.Editor agiUnionEditor = agiUnionSP.edit();
            if(Global.Configuration.model == Status.Model.CARRY)
                agiUnionEditor.putString(MODEL, "carry");
            else
                agiUnionEditor.putString(MODEL, "vehicle");
            agiUnionEditor.commit();
            monitorHelper.stopService(this);
            for (MonitorDevice device : DeviceManager.getInstance().getAllDevices()) {
                try {
                    device.disconnect();
                    DeviceManager.getInstance().remove(device.getName());
                } catch (Exception e) {
                }
            }

            Global.LogInfo.endTime = new Date().toString();
            logInfoManager.addLogInfo();
            dmgr.closeDB();
            cmgr.closeDB();
            logInfoManager.closeDB();
            finish();
            System.exit(0);
        }
    }

    @Override
    protected void onStop() {
        logInfoManager.addLogInfo();
        Log.e("test", "onStop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.e("test", "onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        for (MonitorDevice device : DeviceManager.getInstance().getAllDevices()) {
            if (device.isConnected())
                DeviceManager.getInstance().add(device);
        }
        Log.e("test", "onResume");
        super.onResume();
    }
//    @Override
//    public void onPause() {
//        Log.e("Test", "MainMenuActivity onPause");
//        super.onPause();
//    }
//
//    @Override
//    public void onStop() {
//        Log.e("Test", "MainMenuActivity onStop");
//        super.onStop();
//    }

    class ItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            HashMap<String, Object> item = (HashMap<String, Object>) parent.getItemAtPosition(position);
            String itemText = (String) item.get("itemText");
            Object object = item.get("itemImage");
            //Toast.makeText(MainMenuActivity.this,itemText,Toast.LENGTH_LONG).show();
            stopConnecting = true;
            switch (images[position]) {
                case R.drawable.cell_monitor:
                    startActivity(new Intent(MainMenuActivity.this, CellMonitorActivity.class));
                    break;
                case R.drawable.find_stmsi:
                    startActivity(new Intent(MainMenuActivity.this, FindSTMSIActivity.class));
                    break;
                case R.drawable.orientation:
                    startActivity(new Intent(MainMenuActivity.this, OrientationFindingActivity.class));
                    break;
                case R.drawable.interference:
                    startActivity(new Intent(MainMenuActivity.this, InterferenceActivity.class));
                    break;
                case R.drawable.configuration:
                    startActivity(new Intent(MainMenuActivity.this, ConfigurationActivity.class));
                    break;
                case R.drawable.local_info:
                    startActivity(new Intent(MainMenuActivity.this, LocalInfoActivity.class));
                    break;
            }
        }
    }

    class rebootBtnOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            new AlertDialog.Builder(MainMenuActivity.this)
                    .setTitle(R.string.title_main_menu_device_reboot_confirm)
                    .setPositiveButton(R.string.page_main_menu_device_reboot_confirm_cancel, null)
                    .setNegativeButton(R.string.page_main_menu_device_reboot_confrim_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            rebootConfirm();
                        }
                    }).show();

        }
    }

    private void rebootConfirm() {
        stopConnecting = true;
        for (MonitorDevice device : DeviceManager.getInstance().getDevices()) {
            device.reboot();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectBtn.setEnabled(true);
                Toast.makeText(MainMenuActivity.this, "设备重启中!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    class conBtnOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    stopConnecting = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            connectBtn.setEnabled(false);
                        }
                    });
                    for (int i = 0; i < 10; i++) {
                        if (stopConnecting)
                            break;
                        MainMenuActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                for (MonitorDevice device : DeviceManager.getInstance().getAllDevices()) {
                                    try {
                                        if (!device.isConnected()) {
                                            device.connect();
                                        }
                                        Thread.sleep(200);
                                        if (device.isConnected()) {
                                            DeviceManager.getInstance().add(device);
                                            Log.e("Test", "连接设备：" + device.getName());
                                        }
                                    } catch (Exception e) {
                                    }
                                }
                            }
                        });
                        Log.e("Test", "连接设备数：" + String.valueOf(DeviceManager.getInstance().getDevices().size()));
                        try {
                            Thread.sleep(5000);
                        } catch (Exception e) {
                        }
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            connectBtn.setEnabled(true);
                        }
                    });
                }
            }).start();
        }
    }

    class disconBtnOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            for (MonitorDevice device : DeviceManager.getInstance().getAllDevices()) {
                try {
                    device.disconnect();
                    Thread.sleep(200);
                    DeviceManager.getInstance().remove(device.getName());
                    stopConnecting = true;
                } catch (Exception e) {
                }
            }
            Log.e("Test", "连接设备数：" + String.valueOf(DeviceManager.getInstance().getDevices().size()));
        }
    }

    private void getDevices() {
        List<DeviceDAO> deviceDAOs = dmgr.listDB();

        for (DeviceDAO dao : deviceDAOs) {
            MonitorDevice device = new MonitorDevice(dao.name, dao.ip, dao.type);
            if (DeviceManager.getInstance().getFromAll(device.getName()) == null) {
                DeviceManager.getInstance().addToAll(device);
            }
            Log.e(TAG, "deviceAdd" + dao.ip);
        }
    }

    private class myBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            if (intent.getAction().equals(MonitorApplication.BROAD_FROM_MAIN_MENU_DEVICE)) {
                Bundle bundle = intent.getExtras();
                final int colorOne = bundle.getInt("colorOne");
                final int colorTwo = bundle.getInt("colorTwo");
                final int colorThree = bundle.getInt("colorThree");
                final int colorFour = bundle.getInt("colorFour");
                final String statusText = bundle.getString("statusText");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        deviceColorOne.setBackgroundColor(colorOne);
                        deviceColorTwo.setBackgroundColor(colorTwo);
                        deviceColorThree.setBackgroundColor(colorThree);
                        deviceColorFour.setBackgroundColor(colorFour);
                        deviceStatusText.setText(statusText);
                    }
                });
            } else if (intent.getAction().equals(MonitorApplication.BROAD_FROM_CONFIGURATION_ACTIVITY)) {
                String flag = intent.getStringExtra("flag");
                switch (flag) {
                    case ConfigurationActivity.ADD_DEVICE_FLAG: {
                        String name = intent.getStringExtra("name");
                        String ip = intent.getStringExtra("ip");
                        Status.BoardType type = (Status.BoardType) intent.getSerializableExtra("type");
                        DeviceManager.getInstance().addToAll(new MonitorDevice(name, ip, type));
                        Log.e(TAG, "AllDevices Size" + DeviceManager.getInstance().getAllDevices().size());
                        break;
                    }
                    case ConfigurationActivity.DELETE_DEVICE_FLAG: {
                        String name = intent.getStringExtra("name");
                        MonitorDevice device = DeviceManager.getInstance().getFromAll(name);
                        if (device != null) {
                            device.release();
                        }
                        DeviceManager.getInstance().remove(name);
                        DeviceManager.getInstance().removeFromAll(name);
                        Log.e(TAG, "AllDevices Size" + DeviceManager.getInstance().getAllDevices().size());
                        break;
                    }
                    case ConfigurationActivity.CHANGE_DEVICE_FLAG: {
                        String name = intent.getStringExtra("name");
                        String ip = intent.getStringExtra("ip");
                        Status.BoardType type = (Status.BoardType) intent.getSerializableExtra("type");
                        MonitorDevice device = DeviceManager.getInstance().getFromAll(name);
                        if (device != null) {
                            device.release();
                        }
                        DeviceManager.getInstance().remove(name);
                        Log.e("Device", "replaceName" + name);
                        DeviceManager.getInstance().addToAll(new MonitorDevice(name, ip, type));
                        Log.e(TAG, "AllDevices Size" + DeviceManager.getInstance().getAllDevices().size());
                        break;
                    }
                    case ConfigurationActivity.REBOOT_DEVICE_FLAG: {
                        String name = intent.getStringExtra("name");
                        MonitorDevice device = DeviceManager.getInstance().getDevice(name);
                        if (device != null) {
                            device.reboot();
                            DeviceManager.getInstance().remove(name);
                            Toast.makeText(MainMenuActivity.this, "设备重启中!", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
        }
    }

    private void refreshDeviceStatusBar() {
        int colorOne = getResources().getColor(R.color.default_color);
        int colorTwo = getResources().getColor(R.color.default_color);
        int colorThree = getResources().getColor(R.color.default_color);
        int colorFour = getResources().getColor(R.color.default_color);
        String statusText = "未就绪";
        int i = 0;
        for (MonitorDevice device : DeviceManager.getInstance().getAllDevices()) {
            i++;
            if (device.isConnected()) {
                if (i == 1)
                    colorOne = Color.GREEN;
                else if (i == 2)
                    colorTwo = Color.GREEN;
                else if (i == 3)
                    colorThree = Color.GREEN;
                else if (i == 4)
                    colorFour = Color.GREEN;
                statusText = "已连接";

            } else {
                if (device.getPingStatus() == Status.PingResult.SUCCEED) {
                    if (i == 1)
                        colorOne = Color.YELLOW;
                    else if (i == 2)
                        colorTwo = Color.YELLOW;
                    else if (i == 3)
                        colorThree = Color.YELLOW;
                    else if (i == 4)
                        colorFour = Color.YELLOW;
                    if (statusText != "已连接")
                        statusText = "已就绪";
                } else {
                    if (i == 1)
                        colorOne = Color.RED;
                    else if (i == 2)
                        colorTwo = Color.RED;
                    else if (i == 3)
                        colorThree = Color.RED;
                    else if (i == 4)
                        colorFour = Color.RED;
                }
                try {
                    //device.connect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //Log.d("changeDevice","devicename:"+device.getName()+" hashcode:"+device.hashCode());
        }
        if (DeviceManager.getInstance().getAllDevices().size() == 0) {
            statusText = "无可用设备";
        }
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putInt("colorOne", colorOne);
        bundle.putInt("colorTwo", colorTwo);
        bundle.putInt("colorThree", colorThree);
        bundle.putInt("colorFour", colorFour);
        bundle.putString("statusText", statusText);
        intent.putExtras(bundle);
        intent.setAction(MonitorApplication.BROAD_FROM_MAIN_MENU_DEVICE);
        sendBroadcast(intent);
    }

    private void saveToCache() {
        Global.Configuration.name = Global.UserInfo.user_name;
        Global.Configuration.type = (dao == null ? Status.TriggerType.SMS : dao.type);
        Global.Configuration.smsType = (dao == null ? Status.TriggerSMSType.INSIDE : dao.smsType);
        Global.Configuration.insideSMSType = (dao == null ? Status.InsideSMSType.SILENT : dao.insideSMSType);
        Global.Configuration.silentSMSType = (dao == null ? Status.SilentSMSType.TYPE_ONE : dao.silentSMSType);
        Global.Configuration.triggerInterval = (dao == null ? DEFAULT_TRIGGER_INTERVAL_SMS_MAX : dao.triggerInterval);
        Global.Configuration.filterInterval = DEFAULT_SMS_FILTER_INTERVAL_MAX;
        Global.Configuration.silenceCheckTimer = DEFAULT_SILENCECHECKTIME;
        Global.Configuration.receivingAntennaNum = DEFAULT_RECEIVINGANTENNANUM;
        Global.Configuration.triggerTotalCount = DEFAULT_TOTAL_TRIGGER_COUNT;
        Global.Configuration.targetPhoneNum = (dao == null ? "" : dao.targetPhoneNum);
        Global.Configuration.smsCenter = (dao == null ? "" : dao.smsCenter);

    }

    private void saveToDAO() {
        ConfigurationDAO dao = new ConfigurationDAO(Global.Configuration.name, Global.Configuration.type, Global.Configuration.smsType,
                Global.Configuration.insideSMSType, Global.Configuration.silentSMSType, Global.Configuration.triggerInterval,
                Global.Configuration.filterInterval, Global.Configuration.silenceCheckTimer, Global.Configuration.receivingAntennaNum,
                Global.Configuration.triggerTotalCount, Global.Configuration.targetPhoneNum, Global.Configuration.smsCenter);
        cmgr.insertOrUpdate(dao);
    }
}

