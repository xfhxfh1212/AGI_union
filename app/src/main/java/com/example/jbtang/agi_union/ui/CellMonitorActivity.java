package com.example.jbtang.agi_union.ui;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jbtang.agi_union.R;
import com.example.jbtang.agi_union.core.CellInfo;
import com.example.jbtang.agi_union.core.Global;
import com.example.jbtang.agi_union.core.Status;
import com.example.jbtang.agi_union.dao.cellinfos.CellInfoDAO;
import com.example.jbtang.agi_union.dao.cellinfos.CellInfoDBManager;
import com.example.jbtang.agi_union.dao.confirmed.ConfirmedDAO;
import com.example.jbtang.agi_union.dao.confirmed.ConfirmedDBManager;
import com.example.jbtang.agi_union.device.DeviceManager;
import com.example.jbtang.agi_union.device.MonitorDevice;
import com.example.jbtang.agi_union.external.MonitorApplication;
import com.example.jbtang.agi_union.external.MonitorHelper;
import com.example.jbtang.agi_union.service.CellMonitor;


import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.fmaster.GSMNCellListMessage;
import io.fmaster.GSMServCellMessage;
import io.fmaster.LTENcellInfo;
import io.fmaster.LTENcellListMessage;
import io.fmaster.LTEPwrInfoMessage;
import io.fmaster.LTEServCellMessage;

public class CellMonitorActivity extends AppCompatActivity {
    private Intent startIntent;
    private static final String TAG = "CellMonitorActivity";
    private EditText manualEARFCN;
    private EditText manualPCI;
    private CheckBox manualChoose;
    private EditText alarmTAC;
    private EditText alarmECGI;
    private CheckBox alarmChoose;

    private ListView listView;
    private ListView confirmListView;
    private List<CellInfo> updatingCellInfoList;//更新时
    private List<CellInfo> cellInfoList;//用于显示的邻区表
    private Set<CellInfo> monitorCellSet;//守控小区-手动点击
    private List<CellInfo> monitorCellList;//守控小区-用于显示
    private TextView deviceStatusText;
    private TextView deviceColorOne;
    private TextView deviceColorTwo;
    private TextView deviceColorThree;
    private TextView deviceColorFour;
    private MonitorHelper monitorHelper;
    private CellInfoDBManager cellInfoDBManager;
    private List<CellInfo> cellInfoDBList;
    private ConfirmedDBManager confirmedDBManager;
    private List<CellInfo> confirmedDBList;
    private TextToSpeech textToSpeech;
    private Date alarmTime;
    private SharedPreferences agiUnionSP;
    private final static String CELLMONITOR_DERECTORY = "cellmonitor";
    private final static String MANUALEARFCN = "manualearfcn";
    private final static String MANUALPCI = "manualpci";
    private final static String MANUALCHOOSE = "manualchoose";
    private final static String ALARMTAC = "alarmtac";
    private final static String ALARMECGI = "alarmecgi";
    private final static String ALARMCHOOSE = "alarmchoose";
    private static final String PREFERENCE_PACKAGE = "com.example.jbtang.agi_4buffer";
    private static final String FINDSTMSI_DERECTORY = "findstmsi";
    private static final String TARGETPCI = "targetPci";
    private static final String TARGETEARFCN = "targetEarfcn";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        setContentView(R.layout.activity_cell_monitor);

        initData();
        startService();
    }

    @Override
    public void onDestroy() {
        cellInfoDBManager.clear();
        cellInfoDBManager.add(cellInfoDBList);
        cellInfoDBManager.closeDB();

        confirmedDBManager.clear();
        confirmedDBManager.add(monitorCellList);
        confirmedDBManager.closeDB();
        unregisterReceiver(receiver);
        //unbindService(connection);
        //stopService(startIntent);
        monitorHelper.unbindservice(CellMonitorActivity.this);
        textToSpeech.shutdown();
        SharedPreferences.Editor editor = agiUnionSP.edit();

        editor.putString(MANUALEARFCN,manualEARFCN.getText().toString());
        editor.putString(MANUALPCI,manualPCI.getText().toString());
        if (manualChoose.isChecked()) {
            editor.putString(MANUALCHOOSE, "true");
        } else {
            editor.putString(MANUALCHOOSE, "false");
        }
        editor.putString(ALARMTAC,alarmTAC.getText().toString());
        editor.putString(ALARMECGI,alarmECGI.getText().toString());
        if (alarmChoose.isChecked()) {
            editor.putString(ALARMCHOOSE, "true");
        } else {
            editor.putString(ALARMCHOOSE, "false");
        }

        editor.commit();
        super.onDestroy();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_cell_monitor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.menu_device_configuration_save) {
            saveToNext();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void initData() {
        MyApp = (MonitorApplication) getApplication();
        IntentFilter filter = new IntentFilter(MonitorApplication.BROAD_TO_MAIN_ACTIVITY);
        filter.addAction(MonitorApplication.BROAD_FROM_MAIN_MENU_DEVICE);
        registerReceiver(receiver, filter);

        manualEARFCN = (EditText) findViewById(R.id.cell_monitor_manual_earfcn);
        manualPCI = (EditText) findViewById(R.id.cell_moitor_manual_pci);
        manualChoose = (CheckBox) findViewById(R.id.cell_moitor_manual_choose);

        alarmTAC = (EditText) findViewById(R.id.cell_monitor_alarm_manual_tac);
        alarmECGI = (EditText) findViewById(R.id.cell_moitor_alarm_manual_ecgi);
        alarmChoose = (CheckBox) findViewById(R.id.cell_moitor_alarm_manual_choose);

        agiUnionSP =  getSharedPreferences(CELLMONITOR_DERECTORY, Context.MODE_PRIVATE);
        manualEARFCN.setText(agiUnionSP.getString(MANUALEARFCN,""));
        manualPCI.setText(agiUnionSP.getString(MANUALPCI,""));
        if(agiUnionSP.getString(MANUALCHOOSE,"false").equals("true")){
            manualChoose.setChecked(true);
        } else {
            manualChoose.setChecked(false);
        }
        if(agiUnionSP.getString(ALARMCHOOSE,"false").equals("true")){
            alarmChoose.setChecked(true);
        } else {
            alarmChoose.setChecked(false);
        }
        alarmTAC.setText(agiUnionSP.getString(ALARMTAC,""));
        alarmECGI.setText(agiUnionSP.getString(ALARMECGI,""));

        cellInfoDBManager = new CellInfoDBManager(this);
        cellInfoDBList = getCellInfoList();
        confirmedDBManager = new ConfirmedDBManager(this);
        confirmedDBList = getConfirmedList();

        cellInfoList = new ArrayList<>();
        updatingCellInfoList = new ArrayList<>();

        listView = (ListView) findViewById(R.id.cell_monitor_listView);
        MyAdapter adapter = new MyAdapter(this);
        listView.setAdapter(adapter);

        monitorCellList = new ArrayList<>();
        confirmListView = (ListView) findViewById(R.id.cell_monitor_confirm_listView);
        MyConfirmAdapter confirmAdapter = new MyConfirmAdapter(this);
        confirmListView.setAdapter(confirmAdapter);

        monitorCellSet = new HashSet<>(confirmedDBList);

        manualChoose.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!validateManualEarfcn()) {
                    buttonView.setChecked(false);
                    return;
                }

                CellInfo info = new CellInfo();
                info.earfcn = Integer.parseInt(manualEARFCN.getText().toString());
                info.pci = Short.parseShort(manualPCI.getText().toString());
                info.isChecked = isChecked;
                if (isChecked) {
                    monitorCellSet.add(info);
                } else {
                    monitorCellSet.remove(info);
                }
            }
        });

        deviceStatusText = (TextView) findViewById(R.id.cell_monitor_device_status_text);
        deviceColorOne = (TextView) findViewById(R.id.cell_monitor_device_background_one);
        deviceColorTwo = (TextView) findViewById(R.id.cell_monitor_device_background_two);
        deviceColorThree = (TextView) findViewById(R.id.cell_monitor_device_background_three);
        deviceColorFour = (TextView) findViewById(R.id.cell_monitor_device_background_four);

        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = textToSpeech.setLanguage(Locale.CHINA);
                    if (result == TextToSpeech.LANG_NOT_SUPPORTED
                            || result == TextToSpeech.LANG_MISSING_DATA) {
                        Toast.makeText(CellMonitorActivity.this, "数据丢失或不支持", Toast.LENGTH_LONG).show();
                    } else {
                        textToSpeech.setPitch(1.5f);
                    }
                }
            }
        });
        alarmTime = new Date();
    }

    private List<CellInfo> getCellInfoList() {
        List<CellInfoDAO> cellInfoDAOList = cellInfoDBManager.listDB();
        List<CellInfo> cellInfoDBList = new ArrayList<>();
        for (CellInfoDAO dao : cellInfoDAOList) {
            CellInfo cellInfo = new CellInfo();
            cellInfo.earfcn = dao.earfcn;
            cellInfo.pci = dao.pci;
            cellInfo.tai = dao.tai;
            cellInfo.ecgi = dao.ecgi;
            cellInfoDBList.add(cellInfo);
        }
        return cellInfoDBList;
    }

    private List<CellInfo> getConfirmedList() {
        List<ConfirmedDAO> confirmedDAOList = confirmedDBManager.listDB();
        List<CellInfo> confirmedDBList = new ArrayList<>();
        for (ConfirmedDAO dao : confirmedDAOList) {
            CellInfo cellInfo = new CellInfo();
            cellInfo.earfcn = dao.earfcn;
            cellInfo.pci = dao.pci;
            cellInfo.tai = dao.tai;
            cellInfo.ecgi = dao.ecgi;
            cellInfo.isChecked = true;
            confirmedDBList.add(cellInfo);
        }
        return confirmedDBList;
    }
//    private MonitorService mBoundService;
//
//    private ServiceConnection connection = new ServiceConnection() {
//
//        @Override
//        public void onServiceDisconnected(ComponentName name) {
//
//            mBoundService = null;
//        }
//
//        @Override
//        public void onServiceConnected(ComponentName name, IBinder service) {
//            mBoundService = ((MonitorService.LocalBinder) service).getService();
//            MonitorApplication.MonitorService = mBoundService;
//        }
//    };

    private void startService() {
        //MonitorApplication.IMEI = getIMEI(this);
        //startIntent = new Intent(this, MonitorService.class);
        monitorHelper = new MonitorHelper();
        monitorHelper.bindService(CellMonitorActivity.this);
        Global.ThreadPool.scheduledThreadPool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                monitorCellList = new ArrayList<>();
                cellInfoList = new ArrayList<>(updatingCellInfoList);
//                for (CellInfo info : cellInfoList) {
//                    if (info.isChecked) {
//                        monitorCellList.add(info);
//                    }
//                }
                monitorCellList.clear();
                for (CellInfo info : monitorCellSet) {
                    monitorCellList.add(info);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((MyConfirmAdapter) confirmListView.getAdapter()).notifyDataSetChanged();
                        ((MyAdapter) listView.getAdapter()).notifyDataSetChanged();
                    }
                });
            }
        }, 1, 1, TimeUnit.SECONDS);
    }


    public static String getIMEI(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        String imei = telephonyManager.getDeviceId();
        return imei != null ? imei : "";
    }

    private final MyBroadcastReceiver receiver = new MyBroadcastReceiver();

    class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.d("Broadcast","recive a broadcast: "+intent.getAction());
            if (intent.getAction().equals("")) {
                return;
            }
            if (intent.getAction().equals(MonitorApplication.BROAD_FROM_MAIN_MENU_DEVICE)) {
                refreshStatusBar(intent);
            } else {
                refreshView(intent);
            }
        }
    }

    private void refreshStatusBar(Intent intent) {
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
                //Log.d("Broadcast","refresh");
            }
        });
    }

    private void refreshView(Intent intent) {

        int flag = intent.getFlags();
        Bundle bundle = intent.getExtras();
        switch (flag) {
            case MonitorApplication.SERVER_CELL_FLAG:
                LTEServCellMessage mServCellMessage = bundle.getParcelable("msg");
                SCELL_refresh(mServCellMessage);
                break;
            case MonitorApplication.POWER_INFO_FLAG:
                LTEPwrInfoMessage mPwrInfoMessage = bundle.getParcelable("msg");
                PWR_INFO_refresh(mPwrInfoMessage);
                break;
            case MonitorApplication.N_CELL_FLAG:
                LTENcellListMessage mNcellListMessage = bundle.getParcelable("msg");
                NCell_refresh(mNcellListMessage);
                break;
            case MonitorApplication.STMSI:
                String stmsi = bundle.getString("msg");
                STMSI_refresh(stmsi);
                break;
            case MonitorApplication.GSM_SERVER_CELL_FLAG:
                GSMServCellMessage mGSMServCellMessage = bundle.getParcelable("msg");
                GSM_SCELL_refresh(mGSMServCellMessage);
                break;
            case MonitorApplication.GSM_N_CELL_FLAG:
                GSMNCellListMessage mgsmNcellListMessage = bundle.getParcelable("msg");
                GSM_NCell_refresh(mgsmNcellListMessage);
                break;
            default:
                break;
        }
    }

    private void SCELL_refresh(LTEServCellMessage mServCellMessage) {
        if (mServCellMessage != null) {
            CellInfo info = new CellInfo();
            info.earfcn = mServCellMessage.getEARFCN();
            info.pci = mServCellMessage.getPCI();
            info.tai = mServCellMessage.getTAC();
            info.ecgi = mServCellMessage.getCellId();
            info.isChecked = monitorCellSet.contains(info);
            if (alarmChoose.isChecked() && alarmTAC.getText() != null && alarmECGI.getText() != null) {
                try {
                    if (info.tai == Short.parseShort(alarmTAC.getText().toString()) && info.ecgi == Integer.parseInt(alarmECGI.getText().toString())) {

                        if (textToSpeech != null && !textToSpeech.isSpeaking()) {
                            textToSpeech.speak("发现目标小区", TextToSpeech.QUEUE_FLUSH, null);
                        }
                        if(new Date().getTime() - alarmTime.getTime() > 5000) {
                            alarmTime = new Date();
                            Toast.makeText(CellMonitorActivity.this, "发现目标小区！", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (info.tai == Short.MAX_VALUE || info.ecgi == Integer.MAX_VALUE) {
                for (CellInfo cellInfo : cellInfoDBList) {
                    if (cellInfo.equals(info)) {
                        info.tai = cellInfo.tai;
                        info.ecgi = cellInfo.ecgi;
                        cellInfoDBList.remove(cellInfo);
                        cellInfoDBList.add(cellInfo);
                        break;
                    }
                }
            } else {
                int i = 0;
                for (CellInfo cellInfo : cellInfoDBList) {
                    if (cellInfo.equals(info)) {
                        cellInfo.tai = info.tai;
                        cellInfo.ecgi = info.ecgi;
                        cellInfoDBList.remove(cellInfo);
                        cellInfoDBList.add(cellInfo);
                        break;
                    }
                    i++;
                }
                if (i == cellInfoDBList.size()) {
                    if (cellInfoDBList.size() == 10) {
                        cellInfoDBList.remove(0);
                        cellInfoDBList.add(info);
                    } else {
                        cellInfoDBList.add(info);
                    }
                }
            }

            if (updatingCellInfoList.isEmpty()) {
                updatingCellInfoList.add(info);
            } else {
                updatingCellInfoList.set(0, info);
            }
        }
    }

    private void PWR_INFO_refresh(LTEPwrInfoMessage mPwrInfoMessage) {
        if (mPwrInfoMessage != null) {
            if (!updatingCellInfoList.isEmpty()) {
                updatingCellInfoList.get(0).sinr = mPwrInfoMessage.getSINR();
                updatingCellInfoList.get(0).rsrp = mPwrInfoMessage.getRSRP();

            }
        }
    }

    private void STMSI_refresh(String stmsi) {
        if (stmsi != null && !stmsi.isEmpty()) {
            //stmsiTextView.setText("");
            //stmsiTextView.append("STMSI: " + stmsi);
        }
    }

    private void NCell_refresh(LTENcellListMessage mNcellListMessage) {
        if (mNcellListMessage != null) {
            List<CellInfo> ret = new ArrayList<>();
            for (LTENcellInfo ncellInfo : mNcellListMessage.getNcells()) {
                CellInfo info = new CellInfo();
                info.earfcn = ncellInfo.getEARFCN();
                info.pci = (short) ncellInfo.getPCI();
                info.rsrp = ncellInfo.getRSRP();
                for (CellInfo cellInfo : cellInfoDBList) {
                    if (info.equals(cellInfo)) {
                        info.tai = cellInfo.tai;
                        info.ecgi = cellInfo.ecgi;
                        break;
                    }
                }
                info.isChecked = monitorCellSet.contains(info);
                if(info.isChecked) {
                    monitorCellSet.remove(info);
                    monitorCellSet.add(info);
                }
                ret.add(info);
            }

            if (ret.size() > 1) {
                synchronized (updatingCellInfoList) {
                    if (!updatingCellInfoList.isEmpty()) {
                        CellInfo serveCell = updatingCellInfoList.get(0);
                        updatingCellInfoList = new ArrayList<>(ret);
                        updatingCellInfoList.set(0, serveCell);
                    }
                }
            }
        }
    }

    private void GSM_NCell_refresh(GSMNCellListMessage mNcellListMessage) {
        if (mNcellListMessage != null) {
            //gsmServeCell.setText("");
            //gsmServeCell.append("GSM Ncell info: count " + mNcellListMessage.getNcells().size());
        }

    }

    private void GSM_SCELL_refresh(GSMServCellMessage mServCellMessage) {
        if (mServCellMessage != null) {
            Integer EARFCN = mServCellMessage.getEARFCN();
            //gsmNcell.setText("");
            //gsmNcell.append("GSM serve cell, " + EARFCN);
        }
    }

    public MonitorApplication MyApp;


    private boolean validateManualEarfcn() {
        if (manualEARFCN.getText().toString().isEmpty() || manualPCI.getText().toString().isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("非法输入")
                    .setMessage("请确认'EARFCN'及'PCI'是否输入正确！")
                    .setPositiveButton("确定", null)
                    .show();
            return false;
        }
        return true;
    }

    /**
     * for ListView
     */
    public final class ViewHolder {
        public TextView earfcn;
        public TextView pci;
        public TextView tai;
        public TextView ecgi;
        public TextView sinr;
        public TextView rsrp;
        public CheckBox choose;
    }

    public class MyAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        public MyAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return cellInfoList.size();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return 0;
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.cell_monitor_list_item, null);
                holder = new ViewHolder();
                holder.earfcn = (TextView) convertView.findViewById(R.id.cell_monitor_item_earfcn);
                holder.pci = (TextView) convertView.findViewById(R.id.cell_monitor_item_pci);
                holder.tai = (TextView) convertView.findViewById(R.id.cell_monitor_item_tai);
                holder.ecgi = (TextView) convertView.findViewById(R.id.cell_monitor_item_ecgi);
                holder.sinr = (TextView) convertView.findViewById(R.id.cell_monitor_item_sinr);
                holder.rsrp = (TextView) convertView.findViewById(R.id.cell_monitor_item_rsrp);
                holder.choose = (CheckBox) convertView.findViewById(R.id.cell_monitor_item_choose);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final Integer earfcn = cellInfoList.get(position).earfcn;
            if (earfcn == Integer.MAX_VALUE) {
                holder.earfcn.setText(CellInfo.NULL_VALUE);
            } else {
                holder.earfcn.setText("" + earfcn);
            }

            final Short pci = cellInfoList.get(position).pci;
            if (pci == Short.MAX_VALUE) {
                holder.pci.setText(CellInfo.NULL_VALUE);
            } else {
                holder.pci.setText("" + pci);
            }

            final Short tai = cellInfoList.get(position).tai;
            if (tai == Short.MAX_VALUE) {
                holder.tai.setText(CellInfo.NULL_VALUE);
            } else if (tai >= 0) {
                holder.tai.setText("" + tai);
            } else if (tai < 0) {
                int utai = tai & 0x0000FFFF;
                holder.tai.setText("" + utai);
            }

            final Integer ecgi = cellInfoList.get(position).ecgi;
            if (ecgi == Integer.MAX_VALUE) {
                holder.ecgi.setText(CellInfo.NULL_VALUE);
            } else if (ecgi >= 0) {
                holder.ecgi.setText("" + ecgi);
            } else if (ecgi < 0) {
                long uecgi = ecgi & 0x00000000ffffffff;
                holder.ecgi.setText("" + uecgi);
            }
            NumberFormat nf = NumberFormat.getInstance();
            nf.setMaximumIntegerDigits(3);
            nf.setMaximumFractionDigits(1);


            final Float sinr = cellInfoList.get(position).sinr;
            if (!Float.isNaN(sinr)) {
                holder.sinr.setText(nf.format(sinr));
            } else {
                holder.sinr.setText(CellInfo.NULL_VALUE);
            }
            final Float rsrp = cellInfoList.get(position).rsrp;
            if (!Float.isNaN(rsrp)) {
                holder.rsrp.setText(nf.format(rsrp));
            } else {
                holder.rsrp.setText(CellInfo.NULL_VALUE);
            }

            holder.choose.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    CellInfo info = cellInfoList.get(position);
                    info.isChecked = isChecked;

                    if (isChecked) {
                        monitorCellSet.add(info);
                    } else {
                        monitorCellSet.remove(info);
                    }
                }
            });
            holder.choose.setChecked(monitorCellSet.contains(cellInfoList.get(position)));

            return convertView;
        }
    }

    private void saveToNext() {
        Map<Status.BoardType, List<MonitorDevice>> deviceMap = distributeMonitorDevices();
        Map<Status.BoardType, List<CellInfo>> cellInfoMap = distributeCellInfo();
        if (validate(deviceMap, cellInfoMap)) {

            distributeToMonitor(deviceMap, cellInfoMap);

            Intent intent = new Intent(this, MainMenuActivity.class);
            startActivity(intent);
        }
    }

    private boolean validate(Map<Status.BoardType, List<MonitorDevice>> deviceMap,
                             Map<Status.BoardType, List<CellInfo>> cellInfoMap) {
        if (cellInfoMap.get(Status.BoardType.FDD).size() == 0 && cellInfoMap.get(Status.BoardType.TDD).size() == 0) {
            new AlertDialog.Builder(this)
                    .setTitle("非法配置")
                    .setMessage("未选择守控小区!")
                    .setPositiveButton("确定", null)
                    .show();
            return false;
        }
        for (Status.BoardType type : Status.BoardType.values()) {
            if (deviceMap.get(type).size() < cellInfoMap.get(type).size()) {
                new AlertDialog.Builder(this)
                        .setTitle("非法配置")
                        .setMessage(String.format("%s 模式缺少可用板卡!", type.name()))
                        .setPositiveButton("确定", null)
                        .show();
                return false;
            }
        }
        return true;
    }

    private void distributeToMonitor(Map<Status.BoardType, List<MonitorDevice>> deviceMap,
                                     Map<Status.BoardType, List<CellInfo>> cellInfoMap) {
        for (MonitorDevice device : DeviceManager.getInstance().getDevices()) {
            device.setCellInfo(null);
        }
        for (Status.BoardType type : Status.BoardType.values()) {
            List<CellInfo> cellInfoList = cellInfoMap.get(type);
            List<MonitorDevice> monitorDeviceList = deviceMap.get(type);
            for (int index = 0; index < cellInfoList.size(); index++) {
                CellMonitor.getInstance().prepareMonitor(deviceMap.get(type).get(monitorDeviceList.size() - index - 1), cellInfoList.get(index));
            }
        }
        for (MonitorDevice device : DeviceManager.getInstance().getDevices()) {
            if(device.getCellInfo() != null) {
                Log.e("Test", device.getName() + ":" + device.getCellInfo().earfcn + " " + device.getCellInfo().pci);
            } else {
                Log.e("Test", device.getName() + ":null");
            }
        }
    }

    private Map<Status.BoardType, List<MonitorDevice>> distributeMonitorDevices() {
        Map<Status.BoardType, List<MonitorDevice>> ret = new HashMap<>();
        for (Status.BoardType type : Status.BoardType.values()) {
            ret.put(type, new ArrayList<MonitorDevice>());
        }
        for (MonitorDevice device : DeviceManager.getInstance().getDevices()) {

            ret.get(device.getType()).add(device);
            //Log.e("Test", device.getName());
        }
        for (Status.BoardType type : Status.BoardType.values()) {
            for(MonitorDevice device : ret.get(type)){
                Log.e("Test", device.getName());
            }
        }
        return ret;
    }

    private Map<Status.BoardType, List<CellInfo>> distributeCellInfo() {
        Map<Status.BoardType, List<CellInfo>> ret = new HashMap<>();
        for (Status.BoardType type : Status.BoardType.values()) {
            ret.put(type, new ArrayList<CellInfo>());
        }

        for (CellInfo info : monitorCellList) {
            ret.get(info.toBoardType()).add(info);
        }
        for (Status.BoardType type : Status.BoardType.values()) {
            for(CellInfo cellInfo : ret.get(type)){
                Log.e("Test", cellInfo.earfcn + " " + cellInfo.pci);
            }
        }
        return ret;
    }

    /**
     * for confirm ListView
     */
    public final class ConfirmViewHolder {
        public TextView earfcn;
        public TextView pci;
        public TextView tai;
        public CheckBox choose;
    }

    public class MyConfirmAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        public MyConfirmAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return monitorCellList.size();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return 0;
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            final ConfirmViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.cell_monitor_confirm_list_item, null);
                holder = new ConfirmViewHolder();
                holder.earfcn = (TextView) convertView.findViewById(R.id.cell_monitor_confirm_item_earfcn);
                holder.pci = (TextView) convertView.findViewById(R.id.cell_monitor_confirm_item_pci);
                holder.tai = (TextView) convertView.findViewById(R.id.cell_monitor_confirm_item_tai);
                holder.choose = (CheckBox) convertView.findViewById(R.id.cell_monitor_confirm_item_choose);
                convertView.setTag(holder);
            } else {
                holder = (ConfirmViewHolder) convertView.getTag();
            }

            final Integer earfcn = monitorCellList.get(position).earfcn;
            if (earfcn == Integer.MAX_VALUE) {
                holder.earfcn.setText(CellInfo.NULL_VALUE);
            } else {
                holder.earfcn.setText("" + earfcn);
            }

            final Short pci = monitorCellList.get(position).pci;
            if (pci == Short.MAX_VALUE) {
                holder.pci.setText(CellInfo.NULL_VALUE);
            } else {
                holder.pci.setText("" + pci);
            }

            final Short tai = monitorCellList.get(position).tai;
            if (tai == Short.MAX_VALUE) {
                holder.tai.setText(CellInfo.NULL_VALUE);
            } else {
                holder.tai.setText("" + tai);
            }

            holder.choose.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    CellInfo info = monitorCellList.get(position);
                    info.isChecked = isChecked;

                    if (isChecked) {
                        monitorCellSet.add(info);
                    } else {
                        monitorCellSet.remove(info);
                    }
                }
            });
            holder.choose.setChecked(true);
            return convertView;
        }
    }

}
