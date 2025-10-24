package tj.zmt.barcode;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class SearchActivity extends AppCompatActivity {
    private EditText editText;
    private Button button;
    private DatabaseHelper mDatabaseHelper;
    private SQLiteDatabase mSqLiteDatabase;
    CoordinatorLayout coor;
    TableLayout table;
    Activity activity = this;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        coor = (CoordinatorLayout)findViewById(R.id.coors);
        mDatabaseHelper = new DatabaseHelper(this, "barcode.db", null, 1);
        mSqLiteDatabase = mDatabaseHelper.getReadableDatabase();

        editText = (EditText)findViewById(R.id.editText);
        button = (Button)findViewById(R.id.button2);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = editText.getText().toString();
                tableview(text.toUpperCase());
            }
        });

    }

    private void tableview(final String text){
        String sql = "select * from setting_"+MainActivity.login+" where status='0'";
        Cursor cursor = mSqLiteDatabase.rawQuery(sql, null);
        if (cursor.getCount()>0) {
            table = (TableLayout)findViewById(R.id.table);
            table.removeAllViews();
            String txt = "";
            for (int q = 0; q < 1;q++) {
                TableRow trow = new TableRow(this);
                TableLayout.LayoutParams trowParams = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);
                trow.setLayoutParams(trowParams);
                trow.setPadding(2,2,2,2);
                trow.setBackgroundResource(R.drawable.cell_shape);
                int i=1;
                TextView textss = new TextView(this);
                //textss.setBackgroundResource(R.drawable.cell_shape);
                textss.setTextColor(Color.BLACK);
                textss.setPadding(5,5,5,5);
                textss.setText("Код");
                trow.addView(textss,0);
                while (cursor.moveToNext()) {
                    if (i==1) {
                        txt = txt+cursor.getString(cursor.getColumnIndex("field"));
                    }
                    else {
                        txt = txt+","+cursor.getString(cursor.getColumnIndex("field"));
                    }
                    TextView texts = new TextView(this);
                    texts.setBackgroundResource(R.drawable.cell_shape);
                    texts.setTextColor(Color.BLACK);
                    texts.setPadding(5,5,5,5);
                    texts.setText(cursor.getString(cursor.getColumnIndex("value")).replace(" ","\n"));
                    trow.addView(texts,i);
                    i++;
                }

                table.addView(trow, q);
            }
            String[] tms = txt.split(",");

            String sql2 = "select * from info_"+MainActivity.login+" where Inventory_App='"+text+"'";
            Cursor cursor2;
            try {
                if (mSqLiteDatabase.rawQuery(sql2, null).getCount() >= 1) {
                    cursor2 = mSqLiteDatabase.rawQuery(sql2, null);

                    int j=1;
                    while (cursor2.moveToNext()) {
                        TableRow trow = new TableRow(this);
                        TableLayout.LayoutParams trowParams = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);
                        trow.setLayoutParams(trowParams);
                        trow.setPadding(2,2,2,2);
                        trow.setBackgroundResource(R.drawable.cell_shape);
                        TextView textss = new TextView(this);
                        textss.setId(R.id.edit_text);
                        textss.setBackgroundResource(R.drawable.cell_shape);
                        textss.setTextColor(Color.BLACK);
                        textss.setPadding(10,10,10,10);
                        textss.setText(cursor2.getString(cursor2.getColumnIndex("Inventory_App")));
                        trow.addView(textss, 0);
                        for (int s=0;s<tms.length;s++) {
                            TextView texts = new TextView(this);
                            texts.setBackgroundResource(R.drawable.cell_shape);
                            texts.setPadding(10,10,10,10);

                            if (cursor2.getString(cursor2.getColumnIndex(tms[s].toString())).equals("False") || cursor2.getString(cursor2.getColumnIndex(tms[s].toString())).equals("FALSE")) {
                                texts.setTextColor(Color.RED);
                                texts.setText("X");
                                texts.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                            }
                            else if (cursor2.getString(cursor2.getColumnIndex(tms[s].toString())).equals("True") || cursor2.getString(cursor2.getColumnIndex(tms[s].toString())).equals("TRUE")) {
                                texts.setTextColor(Color.GREEN);
                                texts.setText("V");
                                texts.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                            }
                            else {
                                texts.setTextColor(Color.BLACK);
                                texts.setText(cursor2.getString(cursor2.getColumnIndex(tms[s].toString())));
                            }
                            trow.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    v.setBackgroundColor(Color.LTGRAY);
                                    final TextView tt = (TextView)v.findViewById(R.id.edit_text);
                                    AlertDialog.Builder builder = new AlertDialog.Builder(SearchActivity.this);
                                    builder.setTitle("Внимание!").setMessage("Изменить статус "+tt.getText()+" на проверено?")
                                            .setCancelable(false)
                                            .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    final SimpleDateFormat dateFormatter1 = new SimpleDateFormat("yyyy-MM-dd");
                                                    dateFormatter1.setTimeZone(TimeZone.getTimeZone("GMT+5"));
                                                    String data = dateFormatter1.format(new Date());
                                                    String query = "update info_"+MainActivity.login+" set status='True',Date_Check='"+data+"' where Inventory_App='"+tt.getText().toString()+"';";
                                                    mSqLiteDatabase.execSQL(query);
                                                    Toast.makeText(getApplicationContext(),"Статус инвентаря "+tt.getText().toString()+"_"+data+" обновлен.",Toast.LENGTH_SHORT).show();
                                                    tableview(tt.getText().toString());
                                                    prepareRow(tt.getText().toString());
                                                }
                                            })
                                            .setNegativeButton("Нет",
                                                    new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int id) {
                                                            dialog.cancel();
                                                        }
                                                    });
                                    AlertDialog alert = builder.create();
                                    alert.show();
                                }
                            });
                            trow.addView(texts,s+1);
                        }
                        table.addView(trow, j);
                        j++;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }


    private void prepareRow(String app) {
        String sql = "select * from setting_"+MainActivity.login+" where field not in('IP','PORT','Module','Login','Password');";
        Cursor cursor = mSqLiteDatabase.rawQuery(sql, null);
        if (cursor.getCount()>0) {
            String txt = "";
            int i=0;
            while (cursor.moveToNext()) {
                if (i==0) {
                    txt = txt+cursor.getString(cursor.getColumnIndex("field"));
                }
                else {
                    txt = txt+","+cursor.getString(cursor.getColumnIndex("field"));
                }
                i++;
            }
            String[] tms = txt.split(",");
            String sql2 = "select "+txt+" from info_"+MainActivity.login+" where Inventory_App='"+app+"'";
            Cursor cursor2;
            String xmldata="";
            String xmlrow="";
            try {
                xmldata = xmldata + "<DATAPACKET Version=\"2.0\">" +
                        "<METADATA>" +
                        "<FIELDS>";
                for (int q=0;q<tms.length;q++) {
                    if (tms[q].equals("Status")) {
                        xmldata = xmldata + "<FIELD attrname=\"" + tms[q] + "\" fieldtype=\"boolean\" readonly=\"true\" />";
                    }
                    else {
                        xmldata = xmldata + "<FIELD attrname=\"" + tms[q] + "\" fieldtype=\"string\" readonly=\"true\" WIDTH=\"1000\"/>";
                    }
                }
                if (mSqLiteDatabase.rawQuery(sql2, null).getCount() >= 1) {
                    cursor2 = mSqLiteDatabase.rawQuery(sql2, null);
                    //Log.e("CURSOR", DatabaseUtils.dumpCursorToString(cursor2));
                    while (cursor2.moveToNext()) {
                        xmlrow = xmlrow + "<ROW ";
                        for (int s=0;s<tms.length;s++) {
                            xmlrow = xmlrow + tms[s] + "=\"" + cursor2.getString(cursor2.getColumnIndex(tms[s].toString())).replace("&quot;","").replace("&lt;","").replace("&gt;","").replace("\"","").replace("'","").replace("/","").replace("<","").replace(">","") +"\" ";
                        }
                        xmlrow = xmlrow + "/>";
                    }
                }
                xmldata = xmldata + "</FIELDS>" +
                        "<PARAMS/>" +
                        "</METADATA>" +
                        "<ROWDATA>";
                xmldata = xmldata + xmlrow + "</ROWDATA>" +
                        "</DATAPACKET>";
                //Toast.makeText(getApplicationContext(),"online",Toast.LENGTH_SHORT).show();
                String myURL = "";
                if (MainActivity.safe) {
                    myURL = "https://";
                }
                else {
                    myURL = "http://";
                }
                String sqls = "select * from setting_"+MainActivity.login+" where field in('IP','PORT','Module','Login','Password');";
                Cursor cursors = mSqLiteDatabase.rawQuery(sqls, null);
                String host="";
                String hport="";
                String log="";
                String pass="";
                String module="";

                while (cursors.moveToNext()) {
                    switch (cursors.getString(cursors.getColumnIndex("field"))){
                        case "IP":
                            host=cursors.getString(cursors.getColumnIndex("value"));
                            break;
                        case "PORT":
                            hport=cursors.getString(cursors.getColumnIndex("value"));
                            break;
                        case "Module":
                            module=cursors.getString(cursors.getColumnIndex("value"));
                            break;
                        case "Login":
                            log=cursors.getString(cursors.getColumnIndex("value"));
                            break;
                        case "Password":
                            pass=cursors.getString(cursors.getColumnIndex("value"));
                            break;
                        default:
                            break;
                    }
                }
                myURL = myURL + host+":"+hport+"/"+module;
                new SendLoginData(log,pass,myURL,xmldata,0).execute();

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }


    class SendLoginData extends AsyncTask<String,String,Void> {

        String resultString = null;
        private final String mLogin;
        private final String mPassword;
        private final String MyUrl;
        private final String MyData;
        private final int MyVal;
        int responseCode;

        SendLoginData(String login, String password,String myurl,String data,int val) {
            mLogin = login;
            mPassword = password;
            MyUrl = myurl;
            MyData = data;
            MyVal = val;
        }

        @Override
        protected void onPreExecute() {
            Snackbar snackbar = Snackbar.make(coor,"Отправка данных на сервер",Snackbar.LENGTH_INDEFINITE);
            snackbar.show();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                String parammetrs = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                        "<soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">" +
                        "  <soap12:Body>" +
                        "    <Save_XML xmlns=\"http://tempuri.org/\">" +
                        "      <xml_string><![CDATA["+MyData+"]]></xml_string>" +
                        "      <Trans_ID>"+mLogin+"</Trans_ID>" +
                        "    </Save_XML>" +
                        "  </soap12:Body>" +
                        "</soap12:Envelope>";
                byte[] data = null;
                InputStream is = null;

                try {
                    URL url = new URL(MyUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);
                    conn.setConnectTimeout(5000);
                    conn.setRequestProperty("Content-Length", "" + Integer.toString(parammetrs.getBytes().length));
                    conn.setRequestProperty("Content-Type","text/xml");
                    conn.setRequestProperty("User-Agent", "MFSys Barcode Reader");

                    OutputStream os = conn.getOutputStream();
                    data = parammetrs.getBytes("UTF-8");
                    os.write(data);
                    data = null;

                    conn.connect();
                    responseCode = conn.getResponseCode();

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    if (responseCode == 200) {
                        is = conn.getInputStream();

                        byte[] buffer = new byte[8192]; // Такого вот размера буфер
                        // Далее, например, вот так читаем ответ
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            baos.write(buffer, 0, bytesRead);
                        }
                        data = baos.toByteArray();
                        resultString = new String(data, "UTF-8");
                    }
                } catch (MalformedURLException e) {
                    Log.e("1","MalformedURLException:" + e.getMessage());
                    //resultString = "MalformedURLException:" + e.getMessage();
                } catch (IOException e) {
                    Log.e("2","IOException:" + e.getMessage());
                    //resultString = "IOException:" + e.getMessage();
                } catch (Exception e) {
                    Log.e("3","Exception:" + e.getMessage());
                    //resultString = "Exception:" + e.getMessage();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (resultString == null) {
                Snackbar snackbar = Snackbar.make(coor,"Ошибка отправки данных. Данные не обновлены.",Snackbar.LENGTH_LONG);
                snackbar.show();
                return;
            }
            if(responseCode==200) {
                ReadXML xmlparce = new ReadXML();
                resultString = xmlparce.ReadXML("Save_XMLResult",resultString,"");
                //Log.e("STRING",resultString);
                if (resultString.toUpperCase().equals("TRUE")) {
                    Snackbar snackbar = Snackbar.make(coor,"Отправка данных завершена. Данные обновлены.",Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
                else {
                    Snackbar snackbar = Snackbar.make(coor,"Отправка данных завершена. Данные не обновлены.",Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
            }
            else if (responseCode!=200) {
                Snackbar snackbar = Snackbar.make(coor,"Ошибка "+responseCode+". Обратитесь к Администратору.",Snackbar.LENGTH_LONG);
                snackbar.show();
            }
            else {
                Snackbar snackbar = Snackbar.make(coor,"Ошибка подключения к серверу. Проверьте подключение к интернету",Snackbar.LENGTH_LONG);
                snackbar.show();
                if (MyVal==0) {
                    Toast.makeText(getApplicationContext(),"Ошибка подключения к серверу. Отправьте данные вручную.",Toast.LENGTH_LONG).show();
                }
            }
            if (MyVal==0 && resultString != null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (resultString.toUpperCase().equals("TRUE")) {
                    Toast.makeText(getApplicationContext(),"Отправка данных завершена. Данные обновлены.",Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(getApplicationContext(),"Отправка данных завершена. Данные не обновлены.",Toast.LENGTH_LONG).show();
                }
                //Intent intent = new Intent(getApplicationContext(), BarcodeCaptureActivity.class);
                //startActivityForResult(intent, 1);
            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            Intent intent = new Intent();
            setResult(RESULT_OK, intent);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }
}
