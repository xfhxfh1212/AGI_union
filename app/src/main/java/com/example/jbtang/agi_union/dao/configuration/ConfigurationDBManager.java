package com.example.jbtang.agi_union.dao.configuration;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.jbtang.agi_union.core.Status;

/**
 * Created by jbtang on 11/5/2015.
 */
public class ConfigurationDBManager {
    private ConfigurationDBHelper helper;
    private SQLiteDatabase db;

    public ConfigurationDBManager(Context context) {
        helper = new ConfigurationDBHelper(context);
        db = helper.getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS " + ConfigurationDBHelper.TABLE_NAME +
                "(name TEXT PRIMARY KEY, triggerType INTEGER, triggerSMSType INTEGER,insideSMSType INTEGER, silentSMSType INTEGER, " +
                "triggerInterval INTEGER, filterInterval INTEGER,silenceCheckTimer INTEGER, " +
                "receivingAntennaNum INTEGER, totalTriggerCount INTEGER, targetPhoneNum TEXT, smsCenter TEXT)");
    }

    public void add(ConfigurationDAO dao) {
        db.beginTransaction();
        try {
            insertOrUpdate(dao);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void insertOrUpdate(ConfigurationDAO dao) {
        if (!isExistsByName(dao.name)) {
            insert(dao);
        } else {
            update(dao);
        }
    }

    private void insert(ConfigurationDAO dao) {
        db.execSQL("INSERT INTO " + ConfigurationDBHelper.TABLE_NAME + " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{dao.name, dao.type.ordinal(),dao.smsType.ordinal(),dao.insideSMSType.ordinal(),dao.silentSMSType.ordinal(),
                        dao.triggerInterval, dao.filterInterval, dao.silenceCheckTimer, dao.receivingAntennaNum,
                        dao.totalTriggerCount, dao.targetPhoneNum,dao.smsCenter});
    }

    private void update(ConfigurationDAO dao) {
        ContentValues cv = new ContentValues();
        cv.put("triggerType", dao.type.ordinal());
        cv.put("triggerSMSType",dao.smsType.ordinal());
        cv.put("insideSMSType",dao.insideSMSType.ordinal());
        cv.put("silentSMSType",dao.silentSMSType.ordinal());
        cv.put("triggerInterval", dao.triggerInterval);
        cv.put("filterInterval", dao.filterInterval);
        cv.put("silenceCheckTimer", dao.silenceCheckTimer);
        cv.put("receivingAntennaNum", dao.receivingAntennaNum);
        cv.put("totalTriggerCount", dao.totalTriggerCount);
        cv.put("targetPhoneNum", dao.targetPhoneNum);
        cv.put("smsCenter",dao.smsCenter);
        db.update(ConfigurationDBHelper.TABLE_NAME, cv, "name = ?", new String[]{dao.name});
    }

    public boolean isExistsByName(String name) {
        return isExistsByField("name", name);
    }

    private boolean isExistsByField(String field, String value) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM ").append(ConfigurationDBHelper.TABLE_NAME).append(" WHERE ")
                .append(field).append(" =?");

        return isExistsBySQL(sql.toString(), new String[]{value});
    }

    private boolean isExistsBySQL(String sql, String[] selectionArgs) {
        boolean result = false;

        final Cursor c = db.rawQuery(sql, selectionArgs);
        try {
            if (c.moveToFirst()) {
                result = (c.getInt(0) > 0);
            }
        } finally {
            c.close();
        }
        return result;
    }

    public ConfigurationDAO getConfiguration(String name) {
        String sql = "SELECT * FROM " + ConfigurationDBHelper.TABLE_NAME + " where name = ?";

        final Cursor c = db.rawQuery(sql, new String[]{name});
        ConfigurationDAO dao = null;
        while (c.moveToNext()) {
            int type = c.getInt(c.getColumnIndex("triggerType"));
            int smsType = c.getInt(c.getColumnIndex("triggerSMSType"));
            int insideSMSType = c.getInt(c.getColumnIndex("insideSMSType"));
            int silentSMSType = c.getInt(c.getColumnIndex("silentSMSType"));
            int triggerInterval = c.getInt(c.getColumnIndex("triggerInterval"));
            int filterInterval = c.getInt(c.getColumnIndex("filterInterval"));
            int silenceCheckTimer = c.getInt(c.getColumnIndex("silenceCheckTimer"));
            int receivingAntennaNum = c.getInt(c.getColumnIndex("receivingAntennaNum"));
            int totalTriggerCount = c.getInt(c.getColumnIndex("totalTriggerCount"));
            String targetPhoneNum = c.getString(c.getColumnIndex("targetPhoneNum"));
            String smsCenter = c.getString(c.getColumnIndex("smsCenter"));
            dao = new ConfigurationDAO(name, Status.TriggerType.values()[type], Status.TriggerSMSType.values()[smsType],
                    Status.InsideSMSType.values()[insideSMSType],Status.SilentSMSType.values()[silentSMSType],triggerInterval, filterInterval,
                    silenceCheckTimer, receivingAntennaNum, totalTriggerCount, targetPhoneNum, smsCenter);
        }
        return dao;
    }

    public void closeDB() {
        db.close();
    }

    public void dropTable() {
        String sql = "DROP TABLE " + ConfigurationDBHelper.TABLE_NAME;
        db.execSQL(sql);
    }
}
