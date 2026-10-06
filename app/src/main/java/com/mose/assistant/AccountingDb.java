package com.mose.assistant;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class AccountingDb extends SQLiteOpenHelper {
    public static final String[] TYPES = {"فروش", "خرید", "دریافت", "پرداخت", "ورود انبار", "خروج انبار", "حقوق", "دارایی ثابت", "مالیات"};

    public AccountingDb(Context context) { super(context, "mose_accounting.db", null, 1); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE entries(id INTEGER PRIMARY KEY AUTOINCREMENT,type TEXT NOT NULL,title TEXT NOT NULL,amount REAL NOT NULL DEFAULT 0,quantity REAL NOT NULL DEFAULT 0,party TEXT,note TEXT,created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX idx_entries_type ON entries(type)");
        db.execSQL("CREATE INDEX idx_entries_date ON entries(created_at)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }

    public long add(String type, String title, double amount, double quantity, String party, String note) {
        ContentValues v = new ContentValues();
        v.put("type", type); v.put("title", title); v.put("amount", amount); v.put("quantity", quantity);
        v.put("party", party); v.put("note", note); v.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insertOrThrow("entries", null, v);
    }

    public double total(String type) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COALESCE(SUM(amount),0) FROM entries WHERE type=?", new String[]{type})) {
            return c.moveToFirst() ? c.getDouble(0) : 0;
        }
    }

    public double inventoryBalance() {
        return quantity("ورود انبار") - quantity("خروج انبار");
    }

    private double quantity(String type) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COALESCE(SUM(quantity),0) FROM entries WHERE type=?", new String[]{type})) {
            return c.moveToFirst() ? c.getDouble(0) : 0;
        }
    }

    public List<String> recent(int limit) {
        List<String> rows = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT type,title,amount,party FROM entries ORDER BY created_at DESC LIMIT " + limit, null)) {
            while (c.moveToNext()) {
                String party = c.isNull(3) ? "" : " — " + c.getString(3);
                rows.add(c.getString(0) + " | " + c.getString(1) + party + " | " + Math.round(c.getDouble(2)) + " تومان");
            }
        }
        return rows;
    }
}
