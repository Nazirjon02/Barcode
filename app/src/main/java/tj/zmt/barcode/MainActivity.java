package tj.zmt.barcode;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.material.snackbar.Snackbar;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity implements SoundPool.OnLoadCompleteListener {
    TextView txt;
    FileChooser filechooser;
    private DatabaseHelper mDatabaseHelper;
    private SQLiteDatabase mSqLiteDatabase;
    public static String login="";
    public static Boolean safe=false;
    TableLayout table;
    LinearLayout lin;
    CoordinatorLayout coor;
    private boolean colored = false;
    private String filter="";
    private String pos="";
    public static SharedPreferences mSettings;
    public static String ipaddress;
    public static String port;


    final String LOG_TAG = "myLogs";
    final int MAX_STREAMS = 5;

    SoundPool sp;
    int soundIdBeep;


    public static final String APP_PREFERENCES = "mfsysbarcodereader";
    public static final String APP_PREFERENCES_IPADDRESS = "IPAddress";
    public static final String APP_PREFERENCES_PORT = "Port";
    public static boolean APP_PREFERENCES_HTTPS = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mDatabaseHelper = new DatabaseHelper(this, "barcode.db", null, 1);
        mSqLiteDatabase = mDatabaseHelper.getReadableDatabase();
        coor = (CoordinatorLayout)findViewById(R.id.coord);
        if (TextUtils.isEmpty(login)) {
            Intent login_activity = new Intent(this, LoginActivity.class);
            startActivityForResult(login_activity, 2);
        }

        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        //ipaddress = mSettings.getString(APP_PREFERENCES_IPADDRESS,"95.142.82.59");
        //port = mSettings.getString(APP_PREFERENCES_PORT,"8082");

        if (mSettings.getBoolean(String.valueOf(APP_PREFERENCES_HTTPS),false)) {
            safe = true;
        }

        sp = new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC, 0);
        sp.setOnLoadCompleteListener(this);
        soundIdBeep = sp.load(this, R.raw.beep, 1);
    }

    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
        //Log.d(LOG_TAG, "onLoadComplete, sampleId = " + sampleId + ", status = " + status);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    String select = "select * from info_"+login+" where Inventory_APP='"+barcode.displayValue+"'";
                    Cursor cursor = mSqLiteDatabase.rawQuery(select, null);
                    if (cursor.getCount()>0) {
                        final SimpleDateFormat dateFormatter1 = new SimpleDateFormat("yyyy-MM-dd");
                        dateFormatter1.setTimeZone(TimeZone.getTimeZone("GMT+5"));
                        String dat = dateFormatter1.format(new Date());
                        String query = "update info_"+login+" set status='True',Date_check='"+dat+"' where Inventory_APP='"+barcode.displayValue+"';";
                        mSqLiteDatabase.execSQL(query);
                        Toast.makeText(getApplicationContext(),"Статус инвентаря "+barcode.displayValue+" обновлен.",Toast.LENGTH_LONG).show();
                        sp.play(soundIdBeep, 1, 1, 0, 0, 1);
                        tableview(filter);
                        prepareRow(barcode.displayValue);
                    }
                    else {
                        Toast.makeText(getApplicationContext(),"Инвентарь "+barcode.displayValue+" не найден!",Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(getApplicationContext(), BarcodeCaptureActivity.class);
                        startActivityForResult(intent, 1);
                    }
                } else {
                    Toast.makeText(getApplicationContext(),R.string.no_barcode_captured,Toast.LENGTH_LONG).show();
                }
            } else {
                //Log.e("log", String.format(getString(R.string.barcode_error_format), CommonStatusCodes.getStatusCodeString(resultCode)));
                Toast.makeText(getApplicationContext(),getString(R.string.barcode_error_format),Toast.LENGTH_LONG).show();
            }
        }
        if (requestCode==2 && resultCode==RESULT_OK) {
            Toast.makeText(getApplicationContext(),"Успешная авторизация!",Toast.LENGTH_SHORT).show();
            create();
        }
        if (requestCode==3 && resultCode==RESULT_OK) {
            create();
            //tableview(filter);
        }

        if (resultCode == RESULT_CANCELED && requestCode==2) {
            finish();
            System.exit(0);
        }
    }
    public void create() {
        String sql1 = "select * from setting_"+login+" where status='1'";
        Cursor cursor1 = mSqLiteDatabase.rawQuery(sql1, null);

        if (cursor1.getCount()>0) {
            lin = (LinearLayout) findViewById(R.id.lin);
            assert lin != null;
            lin.removeAllViews();
            String tetx="";
            String tet="";
            int ii=0;
            while (cursor1.moveToNext()){
                if (ii==0) {
                    tet = tet+cursor1.getString(cursor1.getColumnIndex("field"));
                    tetx = tetx+cursor1.getString(cursor1.getColumnIndex("value"));
                }
                else {
                    tet = tet+","+cursor1.getString(cursor1.getColumnIndex("field"));
                    tetx = tetx+","+cursor1.getString(cursor1.getColumnIndex("value"));
                }
                ii++;
            }
            String sql3 = "select " + tet + " from info_"+login+" group by " + tet;
            Cursor cursor3;
            String[] teta = tet.split(",");
            String[] tetb = tetx.split(",");
            //Log.e("SQL",sql3);
            try {
                if (mSqLiteDatabase.rawQuery(sql3, null).getCount() >= 1) {
                    cursor3 = mSqLiteDatabase.rawQuery(sql3, null);
                    cursor3.moveToFirst();
                    //Log.e("CURSOR", DatabaseUtils.dumpCursorToString(cursor3));

                    for (int a=0;a<teta.length;a++) {
                        TextView txtview = new TextView(this);
                        txtview.setText(tetb[a]);
                        txtview.setTextColor(Color.BLACK);
                        txtview.setPadding(0,0,20,0);

                        LinearLayout liner = new LinearLayout(this);
                        liner.setPadding(0,5,0,8);
                        liner.setOrientation(LinearLayout.HORIZONTAL);
                        liner.setVerticalScrollBarEnabled(true);

                        List<String> list = new ArrayList<String>();
                        list.add("Все");
                        String sqls = "select " + teta[a] + " from info_"+login+" group by " + teta[a];
                        Cursor cursors = mSqLiteDatabase.rawQuery(sqls, null);

                        while (cursors.moveToNext()) {
                            list.add(cursors.getString(cursors.getColumnIndex(teta[a])));
                        }

                        final Spinner spinner = new Spinner(this);
                        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
                        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinner.setPrompt(teta[a]);
                        spinner.setPadding(0,3,0,3);
                        spinner.setAdapter(spinnerArrayAdapter);
                        liner.addView(txtview,0);
                        liner.addView(spinner,1);
                        lin.addView(liner,a);
                    }
                    Button btn = new Button(this);
                    btn.setText("Применить фильтр");
                    btn.setTextColor(Color.WHITE);
                    btn.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    btn.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int count = lin.getChildCount();
                            View l = null;
                            filter="";
                            int s=0;
                            pos = "";
                            for(int i=0; i<count; i++) {
                                l = lin.getChildAt(i);
                                if (l instanceof LinearLayout) {
                                    LinearLayout linear = (LinearLayout) l;
                                    View ll = null;
                                    for (int a=0;a<linear.getChildCount();a++) {
                                        ll = linear.getChildAt(a);
                                        if (ll instanceof Spinner) {
                                            Spinner spin = (Spinner) ll;
                                            pos = pos + spin.getSelectedItemPosition() +";";
                                            if (spin.getSelectedItem() != "Все") {
                                                if (s == 0) {
                                                    filter = filter + spin.getPrompt() + "='" + spin.getSelectedItem() + "' ";
                                                } else {
                                                    filter = filter + " and " + spin.getPrompt() + "='" + spin.getSelectedItem() + "' ";
                                                }
                                                s++;
                                            }
                                        }
                                    }
                                }
                            }
                            if (filter.length()>0) {
                                filter = " where " + filter;
                            }
                            tableview(filter);
                        }
                    });
                    lin.addView(btn);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        tableview(filter);
    }

    public void clicked(int value) {
        preparefile(value);
    }
    public void load(int value) {
        if (value==0) {
            String myURL = "";
            if (safe) {
                myURL = "https://";
            }
            else {
                myURL = "http://";
            }
            String sqls = "select * from setting_"+login+" where field in('IP','PORT','Module','Login','Password');";
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
            new GetXMLData(log,pass,myURL).execute();
        }
        else {
            processFile();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_xml) {
            String select = "select * from info_"+login;
            //Cursor cursor = mSqLiteDatabase.rawQuery(select, null);
            try {
                if (mSqLiteDatabase.rawQuery(select, null).getCount()>0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Внимание!").setMessage("При загрузке новых данных, текущие данные будут утеряны. Продолжить?")
                            .setCancelable(false)
                            .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    FragmentManager manager = getSupportFragmentManager();
                                    MyDialogFragment1 myDialogFragment = new MyDialogFragment1();
                                    myDialogFragment.show(manager, "dialog");
                                    myDialogFragment.setRetainInstance(true);
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
            } catch (Exception e) {
                FragmentManager manager = getSupportFragmentManager();
                MyDialogFragment1 myDialogFragment = new MyDialogFragment1();
                myDialogFragment.show(manager, "dialog");
                myDialogFragment.setRetainInstance(true);
                e.printStackTrace();
            }
            return true;
        }
        if (id == R.id.action_scan) {
            Intent intent = new Intent(getApplicationContext(), BarcodeCaptureActivity.class);
            startActivityForResult(intent, 1);
            return true;
        }
        if (id == R.id.action_upload) {
            FragmentManager manager = getSupportFragmentManager();
            MyDialogFragment myDialogFragment = new MyDialogFragment();
            myDialogFragment.show(manager, "dialog");
            myDialogFragment.setRetainInstance(true);
            return true;
        }
        if (id == R.id.action_search) {
            Intent intent = new Intent(this, SearchActivity.class);
            startActivityForResult(intent,3);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void processFile() {
        filechooser = new FileChooser(this);
        filechooser.setExtension("xml");
        filechooser.setFileListener(new FileChooser.FileSelectedListener() {
            @Override
            public void fileSelected(final File file) {
                String filename = file.getAbsolutePath();
                openFile(filename);
            }
        });
        filechooser.showDialog();
    }

    private void openFile(String fileName) {
        String data = "";
        try {
            String line;
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            while ((line = br.readLine()) != null) {
                data = data + line;
            }
        } catch (Throwable t) {
            Toast.makeText(getApplicationContext(), "Exception: " + t.toString(), Toast.LENGTH_LONG).show();
        }
        parcexml(data);
    }

    void parcexml(String data) {
        String tmp="";
        try {
            ReadXML xmlparce = new ReadXML();
            String temp = xmlparce.ReadXML("ROWDATA",data,"").replace("\t\t","").replace("\t","").trim();
            String tmps = "";
            String field = "",fields="";
            String[] tmpx = temp.split("/>");
            for (int i=0;i<tmpx.length;i++) {
                if (!TextUtils.isEmpty(tmpx[i])){
                    //Log.e("SPLIT",tmpx[i]);
                    tmps = tmps + tmpx[i] + "></ROW>";
                    //tmps = tmps.replace("&quot;","\"").replace("&lt;","\"").replace("&gt;","\"");
                }
            }
            //Log.e("XML",tmps);
            XmlPullParser xpp = prepareXpp(tmps);
            String query = "SELECT field FROM setting_"+login+" where field not in('IP','PORT','Module','Login','Password');";
            Cursor cursor = mSqLiteDatabase.rawQuery(query, null);
            int a=0;
            while (cursor.moveToNext()) {
                if (a==0) {
                    fields = fields + cursor.getString(cursor.getColumnIndex("field"));
                    field = field + cursor.getString(cursor.getColumnIndex("field")) + " NVARCHAR(1000)";
                }
                else {
                    fields = fields + "," + cursor.getString(cursor.getColumnIndex("field"));
                    field = field + "," + cursor.getString(cursor.getColumnIndex("field")) + " NVARCHAR(1000)";
                }
                a=a+1;
            }
            cursor.close();
            String create = "CREATE TABLE IF NOT EXISTS info_"+login+"(_id INTEGER PRIMARY KEY AUTOINCREMENT, "+ field +");";
            mDatabaseHelper.DropTable("info_"+login);
            mDatabaseHelper.CreateTable(create);
            while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                tmp = "";
                switch (xpp.getEventType()) {
                    // начало тэга
                    case XmlPullParser.START_TAG:
                        for (int i = 0; i < xpp.getAttributeCount(); i++) {
                            if (i==0) {
                                tmp = "'" + xpp.getAttributeValue(i) + "'";
                            }
                            else {
                                tmp = tmp + ",'" + xpp.getAttributeValue(i) + "'";
                            }
                        }
                        break;
                    default:
                        break;
                }
                // следующий элемент
                xpp.next();
                if (!TextUtils.isEmpty(tmp.trim())) {
                    //Log.e("SQL", fields+",  "+tmp);
                    mDatabaseHelper.insert(fields,tmp,"info_"+login);
                }
            }
            //Toast.makeText(getApplicationContext(),"Таблица загружена!",Toast.LENGTH_SHORT).show();
            Snackbar snackbar = Snackbar.make(coor,"Загрузка данных завершена.",Snackbar.LENGTH_LONG);
            snackbar.show();
            create();
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
    }

    XmlPullParser prepareXpp(String data) throws XmlPullParserException {
        // получаем фабрику
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        // включаем поддержку namespace (по умолчанию выключена)
        factory.setNamespaceAware(true);
        // создаем парсер
        XmlPullParser xpp = factory.newPullParser();
        // даем парсеру на вход Reader
        xpp.setInput(new StringReader(data));
        return xpp;
    }

    private void preparefile(int value) {
        String sql = "select * from setting_"+login+" where field not in('IP','PORT','Module','Login','Password');";
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
            String sql2 = "select "+txt+" from info_"+login;
            Cursor cursor2;
            String xmldata="";
            String xmlrow="";
            try {
                if (value!=0) {
                    xmldata = xmldata + "<?xml version=\"1.0\" standalone=\"yes\"?>";
                }
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
                if (value==0) {
                    //Toast.makeText(getApplicationContext(),"online",Toast.LENGTH_SHORT).show();
                    String myURL = "";
                    if (safe) {
                        myURL = "https://";
                    }
                    else {
                        myURL = "http://";
                    }
                    String sqls = "select * from setting_"+login+" where field in('IP','PORT','Module','Login','Password');";
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

                    new SendLoginData(log,pass,myURL,xmldata,1).execute();
                }
                else {
                    writeToFile(xmldata,"Table_info_"+login);
                }
                //Log.e("xmlrow",xmldata);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void prepareRow(String app) {
        String sql = "select * from setting_"+login+" where field not in('IP','PORT','Module','Login','Password');";
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
            String sql2 = "select "+txt+" from info_"+login+" where Inventory_App='"+app+"'";
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
                if (safe) {
                    myURL = "https://";
                }
                else {
                    myURL = "http://";
                }
                String sqls = "select * from setting_"+login+" where field in('IP','PORT','Module','Login','Password');";
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
                Intent intent = new Intent(getApplicationContext(), BarcodeCaptureActivity.class);
                startActivityForResult(intent, 1);
            }
        }
    }


    class GetXMLData extends AsyncTask<String,String,Void> {

        String resultString = null;
        private final String mLogin;
        private final String mPassword;
        private final String MyUrl;
        int responseCode;

        GetXMLData(String login, String password,String myurl) {
            mLogin = login;
            mPassword = password;
            MyUrl = myurl;
        }

        @Override
        protected void onPreExecute() {
            Snackbar snackbar = Snackbar.make(coor,"Идет загрузка данных...",Snackbar.LENGTH_INDEFINITE);
            snackbar.show();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                String parammetrs = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                        "<soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
                        "  <soap12:Body>\n" +
                        "    <get_Data_XML xmlns=\"http://tempuri.org/\">\n" +
                        "      <Login>"+mLogin+"</Login>\n" +
                        "      <Password>"+mPassword+"</Password>\n" +
                        "    </get_Data_XML>\n" +
                        "  </soap12:Body>\n" +
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

                    OutputStream os = conn.getOutputStream();
                    data = parammetrs.getBytes("UTF-8");
                    os.write(data);
                    data = null;

                    conn.connect();
                    responseCode = conn.getResponseCode();

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    //Log.e("RESPONSE",responseCode+"");
                    if (responseCode == 200) {
                        is = conn.getInputStream();
                        byte[] buffer = new byte[4096 * 16];
                        //byte[] buffer = new byte[8192]; // Такого вот размера буфер
                        //byte[] buffer = IOUtils.toByteArray(is);
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
            if(resultString != null) {
                ReadXML xmlparce = new ReadXML();
                resultString = xmlparce.ReadXML("get_Data_XMLResult",resultString,"");
                resultString = resultString.replace("&lt;","<");
                resultString = resultString.replace("&gt;",">");
                Snackbar snackbar = Snackbar.make(coor,"Обработка полученных данных...",Snackbar.LENGTH_INDEFINITE);
                snackbar.show();
                parcexml(resultString);
            }
            else if (responseCode==400 || responseCode==500) {
                Snackbar snackbar = Snackbar.make(coor,"Ошибка "+responseCode+". Обратитесь к Администратору.",Snackbar.LENGTH_LONG);
                snackbar.show();
            }
            else {
                Snackbar snackbar = Snackbar.make(coor,"Ошибка подключения к серверу. Проверьте подключение к интернету",Snackbar.LENGTH_LONG);
                snackbar.show();
            }
        }
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void tableview(String filter){
        String sql = "select * from setting_"+login+" where status='0'";
        Cursor cursor = mSqLiteDatabase.rawQuery(sql, null);

        colored = false;

        if (cursor.getCount()>0) {

            table = (TableLayout)findViewById(R.id.table);
            assert table != null;
            table.removeAllViews();
            String txt = "";
            for (int q = 0; q < 1;q++) {
                TableRow trow = new TableRow(this);
                TableLayout.LayoutParams trowParams = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);
                trow.setLayoutParams(trowParams);
                trow.setPadding(2,2,2,2);
                TextView textss = new TextView(this);
                textss.setBackgroundColor(Color.BLACK);
                textss.setTextColor(Color.WHITE);
                textss.setPadding(5,5,5,5);
                textss.setText("№");
                trow.addView(textss, 0);
                int i=1;
                while (cursor.moveToNext()) {
                    if (i==1) {
                        txt = txt+cursor.getString(cursor.getColumnIndex("field"));
                    }
                    else {
                        txt = txt+","+cursor.getString(cursor.getColumnIndex("field"));
                    }
                    TextView texts = new TextView(this);
                    texts.setBackgroundColor(Color.BLACK);
                    texts.setTextColor(Color.WHITE);
                    texts.setPadding(5,5,5,5);
                    texts.setText(cursor.getString(cursor.getColumnIndex("value")));
                    trow.addView(texts,i);
                    i++;
                }
                table.addView(trow, q);
            }
            String[] tms = txt.split(",");

            String sql2 = "select * from info_"+login+" " + filter + " order by Date_check";
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
                        textss.setTextColor(Color.BLACK);
                        textss.setBackgroundResource(R.drawable.cell_shape);
                        textss.setPadding(5,5,5,5);
                        textss.setText(""+j);
                        trow.addView(textss, 0);
                        for (int s=0;s<tms.length;s++) {
                            TextView texts = new TextView(this);
                            texts.setBackgroundResource(R.drawable.cell_shape);
                            texts.setPadding(5,5,5,5);
                            if (cursor2.getString(cursor2.getColumnIndex(tms[s])).equals("False") || cursor2.getString(cursor2.getColumnIndex(tms[s])).equals("FALSE")) {
                                texts.setTextColor(Color.RED);
                                texts.setText("X");
                                texts.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                            }
                            else if (cursor2.getString(cursor2.getColumnIndex(tms[s])).equals("True") || cursor2.getString(cursor2.getColumnIndex(tms[s])).equals("TRUE")) {
                                texts.setTextColor(Color.GREEN);
                                texts.setText("۷");
                                texts.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                            }
                            else {
                                texts.setTextColor(Color.BLACK);
                                texts.setText(cursor2.getString(cursor2.getColumnIndex(tms[s])).replace("&quot;","\"").replace("&lt;","\"").replace("&gt;","\""));
                            }
                            trow.addView(texts,s+1);
                            trow.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                    //Log.e("COLORED",colored+"");
                                }
                            });
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
    public void writeToFile(String data, String fname) {
        final File path = Environment.getExternalStoragePublicDirectory("/mfsys/");

        if(!path.exists()) {
            path.mkdirs();
        }

        final File file = new File(path, fname+".xml");

        try {
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(data);
            myOutWriter.close();
            fOut.flush();
            fOut.close();
            //Toast.makeText(getApplicationContext(),"Таблица успешно экпортирована в mfsys/"+fname+".xml",Toast.LENGTH_SHORT).show();
            Snackbar snackbar = Snackbar.make(coor,"Таблица успешно экпортирована в mfsys/"+fname+".xml",Snackbar.LENGTH_LONG);
            snackbar.show();
        }
        catch (IOException e) {
            //Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("login", login);
        outState.putString("filter", filter);
        outState.putString("pos", pos);
        //Log.d(LOG_TAG, "onSaveInstanceState");
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        login = savedInstanceState.getString("login");
        filter = savedInstanceState.getString("filter");
        pos = savedInstanceState.getString("pos");
        String sql1 = "select * from setting_"+login+" where status='1'";
        Cursor cursor1 = mSqLiteDatabase.rawQuery(sql1, null);

        if (cursor1.getCount()>0) {
            lin = (LinearLayout) findViewById(R.id.lin);
            assert lin != null;
            lin.removeAllViews();
            String tetx="";
            String tet="";
            int ii=0;
            while (cursor1.moveToNext()){
                if (ii==0) {
                    tet = tet+cursor1.getString(cursor1.getColumnIndex("field"));
                    tetx = tetx+cursor1.getString(cursor1.getColumnIndex("value"));
                }
                else {
                    tet = tet+","+cursor1.getString(cursor1.getColumnIndex("field"));
                    tetx = tetx+","+cursor1.getString(cursor1.getColumnIndex("value"));
                }
                ii++;
            }
            String sql3 = "select " + tet + " from info_"+login+" group by " + tet;
            Cursor cursor3;
            String[] teta = tet.split(",");
            String[] tetb = tetx.split(",");
            String[] tetc = pos.split(";");
            //Log.e("SQL",sql3);
            try {
                if (mSqLiteDatabase.rawQuery(sql3, null).getCount() >= 1) {
                    cursor3 = mSqLiteDatabase.rawQuery(sql3, null);
                    cursor3.moveToFirst();
                    //Log.e("CURSOR", DatabaseUtils.dumpCursorToString(cursor3));

                    for (int a=0;a<teta.length;a++) {
                        TextView txtview = new TextView(this);
                        txtview.setText(tetb[a]);
                        txtview.setTextColor(Color.BLACK);
                        txtview.setPadding(0,0,20,0);

                        LinearLayout liner = new LinearLayout(this);
                        liner.setOrientation(LinearLayout.HORIZONTAL);
                        liner.setVerticalScrollBarEnabled(true);
                        liner.setPadding(0,5,0,8);

                        List<String> list = new ArrayList<String>();
                        list.add("Все");
                        String sqls = "select " + teta[a] + " from info_"+login+" group by " + teta[a];
                        Cursor cursors = mSqLiteDatabase.rawQuery(sqls, null);

                        while (cursors.moveToNext()) {
                            list.add(cursors.getString(cursors.getColumnIndex(teta[a])));
                        }

                        final Spinner spinner = new Spinner(this);
                        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
                        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinner.setPrompt(teta[a]);
                        spinner.setAdapter(spinnerArrayAdapter);
                        spinner.setPadding(0,3,0,3);

                        if (pos.length()>0) {
                            spinner.setSelection(Integer.parseInt(tetc[a]));
                        }
                        liner.addView(txtview,0);
                        liner.addView(spinner,1);
                        lin.addView(liner,a);
                    }
                    Button btn = new Button(this);
                    btn.setText("Применить фильтр");
                    btn.setTextColor(Color.WHITE);
                    btn.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    btn.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int count = lin.getChildCount();
                            View l = null;
                            filter="";
                            int s=0;
                            pos = "";
                            for(int i=0; i<count; i++) {
                                l = lin.getChildAt(i);
                                if (l instanceof LinearLayout) {
                                    LinearLayout linear = (LinearLayout) l;
                                    View ll = null;
                                    for (int a=0;a<linear.getChildCount();a++) {
                                        ll = linear.getChildAt(a);
                                        if (ll instanceof Spinner) {
                                            Spinner spin = (Spinner) ll;
                                            pos = pos + spin.getSelectedItemPosition()+";";
                                            if (spin.getSelectedItem() != "Все") {
                                                if (s == 0) {
                                                    filter = filter + spin.getPrompt() + "='" + spin.getSelectedItem() + "' ";
                                                } else {
                                                    filter = filter + " and " + spin.getPrompt() + "='" + spin.getSelectedItem() + "' ";
                                                }
                                                s++;
                                            }
                                        }
                                    }
                                }
                            }
                            if (filter.length()>0) {
                                filter = " where " + filter;
                            }
                            tableview(filter);
                        }
                    });
                    lin.addView(btn);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        tableview(filter);
    }
}
