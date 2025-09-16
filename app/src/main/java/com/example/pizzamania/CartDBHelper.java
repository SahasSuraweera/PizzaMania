package com.example.pizzamania;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class CartDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "PizzaCart.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_CART = "cart";
    private static final String COL_ID = "id";
    private static final String COL_NAME = "name";
    private static final String COL_SIZE = "size";
    private static final String COL_PRICE = "price";
    private static final String COL_QUANTITY = "quantity";

    public CartDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_CART + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NAME + " TEXT, " +
                COL_SIZE + " TEXT, " +
                COL_PRICE + " REAL, " +
                COL_QUANTITY + " INTEGER, " +
                "UNIQUE(" + COL_NAME + ", " + COL_SIZE + "))";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CART);
        onCreate(db);
    }
    public void addOrUpdateItem(String name, String size, double price, int quantity) {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query(TABLE_CART, null,
                COL_NAME + "=? AND " + COL_SIZE + "=?",
                new String[]{name, size}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int currentQty = cursor.getInt(cursor.getColumnIndexOrThrow(COL_QUANTITY));
            ContentValues values = new ContentValues();
            values.put(COL_QUANTITY, currentQty + quantity);
            db.update(TABLE_CART, values, COL_NAME + "=? AND " + COL_SIZE + "=?",
                    new String[]{name, size});
        } else {
            ContentValues values = new ContentValues();
            values.put(COL_NAME, name);
            values.put(COL_SIZE, size);
            values.put(COL_PRICE, price);
            values.put(COL_QUANTITY, quantity);
            db.insert(TABLE_CART, null, values);
        }

        if (cursor != null) cursor.close();
    }

    public void removeItem(String name, String size) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CART, COL_NAME + "=? AND " + COL_SIZE + "=?", new String[]{name, size});
    }

    public ArrayList<CartItem> getAllItems() {
        ArrayList<CartItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CART, null, null, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME));
                String size = cursor.getString(cursor.getColumnIndexOrThrow(COL_SIZE));
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PRICE));
                int qty = cursor.getInt(cursor.getColumnIndexOrThrow(COL_QUANTITY));
                list.add(new CartItem(name, size, price, qty));
            } while (cursor.moveToNext());
        }

        if (cursor != null) cursor.close();
        return list;
    }

    public double getSubTotal() {
        double total = 0;
        for (CartItem item : getAllItems()) {
            total += item.getPrice() * item.getQuantity();
        }
        return total;
    }

    public void clearCart() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CART, null, null);
    }

    public static class CartItem {
        private String name;
        private String size;
        private double price;
        private int quantity;

        public CartItem(String name, String size, double price, int quantity) {
            this.name = name;
            this.size = size;
            this.price = price;
            this.quantity = quantity;
        }

        public String getName() { return name; }
        public String getSize() { return size; }
        public double getPrice() { return price; }
        public int getQuantity() { return quantity; }
    }
}
