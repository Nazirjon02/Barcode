package tj.zmt.barcode;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private AutoCompleteTextView mLoginView;
    private AutoCompleteTextView mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private TextView txt;
    FileChooser filechooser;
    private int k=0;
    private DatabaseHelper mDatabaseHelper;
    private SQLiteDatabase mSqLiteDatabase;

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mLoginView = (AutoCompleteTextView) findViewById(R.id.login);
        mPasswordView = (AutoCompleteTextView) findViewById(R.id.password);

        mDatabaseHelper = new DatabaseHelper(this, "barcode.db", null, 1);
        mSqLiteDatabase = mDatabaseHelper.getReadableDatabase();

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(LoginActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {

            } else {
                ActivityCompat.requestPermissions(LoginActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
            }
        }
        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        txt = (TextView)findViewById(R.id.textView2);

        Button qr_button = (Button)findViewById(R.id.qr_button);
        qr_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), BarcodeCaptureActivity.class);
                startActivityForResult(intent, 1);
            }
        });
        Button file_button = (Button)findViewById(R.id.file_button);
        file_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                processFile();
            }
        });

        Button server_button = (Button)findViewById(R.id.server_button);
        server_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),Loadsettings.class);
                //startActivity(intent);
                //Intent intent = new Intent(getApplicationContext(), BarcodeCaptureActivity.class);
                startActivityForResult(intent, 2);
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    String dat = barcode.displayValue;
                    ReadXML xmlparce = new ReadXML();
                    dat = xmlparce.ReadXML("SETTINGS",dat,"").replace("\t","");
                    String login = "";
                    String tmp="";
                    try {
                        XmlPullParser xpp = prepareXpp(dat);
                        String text="";
                        while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                            switch (xpp.getEventType()) {
                                // начало тэга
                                case XmlPullParser.START_TAG:
                                    text = text + "('" + xpp.getName() + "',";
                                    tmp = "";
                                    for (int i = 0; i < xpp.getAttributeCount(); i++) {
                                        if (xpp.getName().equals("Login") && xpp.getAttributeName(i).equals("name")) {
                                            login = xpp.getAttributeValue(i);
                                        }
                                        tmp = tmp + "'" + xpp.getAttributeValue(i) + "',";
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
                        txt.setText("Настройки загружены. Можете авторизоваться!");
                    } catch (XmlPullParserException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else txt.setText(R.string.no_barcode_captured);
            } else Log.e("log", String.format(getString(R.string.barcode_error_format),
                    CommonStatusCodes.getStatusCodeString(resultCode)));
        }
        if (requestCode==2) {
            if (resultCode==RESULT_OK) {
                txt.setText("Настройки загружены. Можете авторизоваться!");
            }
            else {
                //txt.setText("Настройки загружены. Можете авторизоваться!");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(this, "Пока вы не предоставите разрешение, мы не можем открыть файлы", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void processFile() {
        filechooser = new FileChooser(LoginActivity.this);
        filechooser.setFileListener(new FileChooser.FileSelectedListener() {
            @Override
            public void fileSelected(final File file) {
                String filename = file.getAbsolutePath();
                openFile(filename);
            }
        });
        filechooser.setExtension("xml");
        filechooser.showDialog();
    }

    private void openFile(String fileName) {
        String data="";
        try {
            String line;
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            while ((line = br.readLine()) != null) {
                data=data+line;
            }
        } catch (Throwable t) {
            Toast.makeText(getApplicationContext(), "Exception: " + t.toString(), Toast.LENGTH_LONG).show();
        }

        String tmp="";
        try {
            ReadXML xmlparce = new ReadXML();
            //Log.e("XML_DATA",data);
            data = xmlparce.ReadXML("SETTINGS",data,"").replace("\t","");
            XmlPullParser xpp = prepareXpp(data);
            //String login = data.substring(data.indexOf("<ID_User name="));
            String login = "";
            //login = login.substring(login.indexOf(">")+1);
            //login = login.substring(0,login.indexOf("<"));
            //Log.e("XML_LOGIN",login);
            String text="";
            while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                switch (xpp.getEventType()) {
                    // начало тэга
                    case XmlPullParser.START_TAG:
                        text = text + "('" + xpp.getName() + "',";
                        tmp = "";
                        for (int i = 0; i < xpp.getAttributeCount(); i++) {
                            //Log.e("LOG-IN",xpp.getName());
                            if (Objects.equals(xpp.getName(), "Login") && Objects.equals(xpp.getAttributeName(i), "name")) {
                                login = xpp.getAttributeValue(i);
                                //Log.e("LOGIN",login);
                            }
                            tmp = tmp + "'" + xpp.getAttributeValue(i) + "',";
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
            Log.e("CREATE",create);
            mDatabaseHelper.DropTable("setting_"+login);
            mDatabaseHelper.CreateTable(create);
            String temp="Insert into setting_"+login+" (field,value,status) values ";
            temp = temp + text.substring(0,text.length()-1)+";";
            mDatabaseHelper.inserts(temp);
            txt.setText("Настройки загружены. Можете авторизоваться!");
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

    private void attemptLogin() {
        mLoginView.setError(null);
        mPasswordView.setError(null);

        String login = mLoginView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError("Пароль слишком короткий");
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(login)) {
            mLoginView.setError(getString(R.string.error_field_required));
            focusView = mLoginView;
            cancel = true;
        }
        else if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            showProgress(true);
            new UserLoginTask(login,password).execute();
        }
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
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
    public class UserLoginTask extends AsyncTask<String,String,Boolean> {

        private final String mLogin;
        private final String mPassword;

        UserLoginTask(String login, String password) {
            mLogin = login;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            boolean state=false;
            state = mDatabaseHelper.select(mLogin,mPassword);
            publishProgress(String.valueOf(state));
            return true;
        }

        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            if (values[0].equals("true")) {
                MainActivity.login = mLoginView.getText().toString();
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                showProgress(false);
                finish();
            }
            else {
                showProgress(false);
                txt.setText("Идентификатор не найден. Загрузите настройки.");
            }
        }
    }
    public void onBackPressed() {
        if (k==1) {
            Intent intent = new Intent();
            setResult(RESULT_CANCELED, intent);
            finish();
            k=0;
        }
        else {
            Toast.makeText(getApplicationContext(),"Нажмите еще раз для выхода",Toast.LENGTH_SHORT).show();
        }
        k=1;
    }
}