package com.example.pizzamania;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class CartDBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "PizzaManiaDB";
    private static final int DB_VERSION = 2;
    private static final String TABLE_NAME = "cart";

    public CartDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CART_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT UNIQUE," +
                "price REAL," +
                "quantity INTEGER)";
        db.execSQL(CREATE_CART_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Add or update item
    public void addOrUpdateItem(String name, double price, int quantity) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("price", price);
        values.put("quantity", quantity);

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE name=?", new String[]{name});
        if (cursor.moveToFirst()) {
            db.update(TABLE_NAME, values, "name=?", new String[]{name});
        } else {
            db.insert(TABLE_NAME, null, values);
        }
        cursor.close();
    }

    // Remove item
    public void removeItem(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, "name=?", new String[]{name});
    }

    // Get all items
    public Cursor getAllItems() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
    }

    // Get subtotal
    public double getSubTotal() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(price * quantity) as subtotal FROM " + TABLE_NAME, null);
        double subtotal = 0;
        if (cursor.moveToFirst()) {
            subtotal = cursor.getDouble(cursor.getColumnIndexOrThrow("subtotal"));
        }
        cursor.close();
        return subtotal;
    }
}