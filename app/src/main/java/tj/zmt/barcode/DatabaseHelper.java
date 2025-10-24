package tj.zmt.barcode;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.provider.BaseColumns;

public class DatabaseHelper extends SQLiteOpenHelper implements BaseColumns {

    private static final String DATABASE_NAME = "barcode.db";
    private static final int DATABASE_VERSION = 1;


    DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public DatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
                          int version) {
        super(context, name, factory, version);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public DatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
                          int version, DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void CreateTable(String table) {
        SQLiteDatabase dbs = this.getWritableDatabase();
        dbs.execSQL(table);
    }

    public void DropTable(String table) {
        SQLiteDatabase dbs = this.getWritableDatabase();
        dbs.execSQL("DROP TABLE IF EXISTS "+table);
    }

    public void insert(String fields, String values, String table) {
        SQLiteDatabase db = this.getWritableDatabase();
        String sql = "insert into "+table+" ("+fields+") values ("+values+");";
        db.execSQL(sql);
    }

    public void inserts(String query) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(query);
    }

    public boolean select(String login, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            String select = "select * from setting_"+login+" where (field='Login' and value='"+login+"') or (field='Password' and value='"+password+"')";
            //Log.e("SELECT",select);
            Cursor cursor = db.rawQuery(select, null);
            //Log.e("CURSOR", DatabaseUtils.dumpCursorToString(cursor));
            if (cursor.getCount() == 2) {
                return true;
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return false;
    }


}