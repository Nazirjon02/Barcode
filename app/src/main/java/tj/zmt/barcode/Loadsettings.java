package tj.zmt.barcode;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Loadsettings extends AppCompatActivity {
    private ProgressBar progress;
    private View mProgressView;
    private View mLoginFormView;
    private DatabaseHelper mDatabaseHelper;
    private SQLiteDatabase mSqLiteDatabase;
    AutoCompleteTextView mlogin;
    AutoCompleteTextView mpassword;
    CheckBox chk;
    public static SharedPreferences mSettings;
    public static String ipaddress;
    public static String por;

    public static final String APP_PREFERENCES = "mfsysbarcodereader";
    public static final String APP_PREFERENCES_IPADDRESS = "IPAddress";
    public static final String APP_PREFERENCES_PORT = "Port";
    public static boolean APP_PREFERENCES_HTTPS = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loadsettings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        ipaddress = mSettings.getString(APP_PREFERENCES_IPADDRESS,"");
        por = mSettings.getString(APP_PREFERENCES_PORT,"");


        mDatabaseHelper = new DatabaseHelper(this, "barcode.db", null, 1);
        mSqLiteDatabase = mDatabaseHelper.getReadableDatabase();

        Button btn = (Button)findViewById(R.id.load);
        chk = (CheckBox)findViewById(R.id.checkBox);

        if (mSettings.getBoolean(String.valueOf(APP_PREFERENCES_HTTPS),false)) {
            chk.setChecked(true);
        }

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        mlogin = (AutoCompleteTextView)findViewById(R.id.login);
        mpassword = (AutoCompleteTextView)findViewById(R.id.password);
        final AutoCompleteTextView ip = (AutoCompleteTextView)findViewById(R.id.ip);
        final AutoCompleteTextView port = (AutoCompleteTextView)findViewById(R.id.port);

        ip.setText(ipaddress);
        port.setText(por);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //String ip = "http://95.142.82.59:8082/Assets_Service.asmx";
                String myURL = "";
                SharedPreferences.Editor editor= MainActivity.mSettings.edit();
                if (chk.isChecked()) {
                    myURL = "https://";
                    MainActivity.safe = true;
                    editor.putString(MainActivity.APP_PREFERENCES_IPADDRESS,ip.getText().toString());
                    editor.putBoolean(String.valueOf(MainActivity.APP_PREFERENCES_HTTPS),true);
                }
                else {
                    myURL = "http://";
                    MainActivity.safe = false;
                    editor.putBoolean(String.valueOf(MainActivity.APP_PREFERENCES_HTTPS),false);
                }
                editor.putString(MainActivity.APP_PREFERENCES_IPADDRESS,ip.getText().toString());
                editor.putString(MainActivity.APP_PREFERENCES_PORT,port.getText().toString());
                editor.apply();
                myURL = myURL + ip.getText().toString()+":"+port.getText().toString()+"/Assets_Service.asmx";
                Log.e("onClick: ",myURL+"" );
                new SendLoginData(mlogin.getText().toString(),mpassword.getText().toString(),myURL).execute();

            }
        });

    }

    class SendLoginData extends AsyncTask<String,String,Void> {

        String resultString = null;
        private final String mLogin;
        private final String mPassword;
        private final String MyUrl;

        SendLoginData(String login, String password,String myurl) {
            mLogin = login;
            mPassword = password;
            MyUrl = myurl;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgress(true);
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                String parammetrs = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                        "<soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">" +
                        "  <soap12:Body>" +
                        "    <get_Settings xmlns=\"http://tempuri.org/\">" +
                        "      <Login>"+mLogin+"</Login>" +
                        "      <Password>"+mPassword+"</Password>" +
                        "    </get_Settings>" +
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

                    OutputStream os = conn.getOutputStream();
                    data = parammetrs.getBytes("UTF-8");
                    os.write(data);
                    data = null;

                    conn.connect();
                    int responseCode = conn.getResponseCode();

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
            if(resultString != null) {
                ReadXML xmlparce = new ReadXML();
                resultString = xmlparce.ReadXML("get_SettingsResult",resultString,"");
                resultString = resultString.replace("&lt;","<");
                resultString = resultString.replace("&gt;",">");
                resultString = xmlparce.ReadXML("SETTINGS",resultString,"");
                //Log.e("resultstring",resultString);
                showProgress(false);
                if (resultString.length()>0) {
                    parcexml(resultString);
                }
                else {
                    Toast.makeText(getApplicationContext(),"Данные не верны. Проверьте логин/пароль.",Toast.LENGTH_LONG).show();
                }
            }
            else {
                Toast.makeText(getApplicationContext(),"Ошибка подключения к серверу. Проверьте подключение к интернету",Toast.LENGTH_LONG).show();
                showProgress(false);
            }
        }
    }

    void parcexml(String data) {
        String tmp="";
        String login = "";
        try {
            XmlPullParser xpp = prepareXpp(data);
            String text="";
            while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                switch (xpp.getEventType()) {
                    // начало тэга
                    case XmlPullParser.START_TAG:
                        text = text + "('" + xpp.getName() + "',";
                        tmp = "";
                        //Log.e("LOG-IN",xpp.getName());
                        for (int i = 0; i < xpp.getAttributeCount(); i++) {
                            if (xpp.getName().equals("Login") && xpp.getAttributeName(i).equals("name")) {
                                login = xpp.getAttributeValue(i);
                            }
                            tmp = tmp + "'" + xpp.getAttributeValue(i) + "',";
                            //Log.e("LOGIN",xpp.getAttributeValue(i));
                        }
                        if (!TextUtils.isEmpty(tmp)) {
                            text = text + tmp;
                        }
                        break;
                    // содержимое тэга
                    case XmlPullParser.TEXT:
                        text = text + "'" + xpp.getText() + "'),";
                        break;
                    default:
                        break;
                }
                // следующий элемент
                xpp.next();
            }
            String create = "CREATE TABLE IF NOT EXISTS setting_"+login+"(_id INTEGER PRIMARY KEY AUTOINCREMENT, field NVARCHAR(255), value NVARCHAR(255),status NVARCHAR(2));";
            mDatabaseHelper.DropTable("setting_"+login);
            mDatabaseHelper.CreateTable(create);
            String temp="Insert into setting_"+login+" (field,value,status) values ";
            temp = temp + text.substring(0,text.length()-1)+";";
            mDatabaseHelper.inserts(temp);
            Intent intent = getIntent();
            setResult(RESULT_OK, intent);
            finish();
            //txt.setText("Настройки загружены. Можете авторизоваться!");
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            Intent intent = new Intent();
            setResult(RESULT_CANCELED, intent);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        finish();
    }

}
