package kr.inha.inti.safe_backhome;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.location.Address;
import android.location.Geocoder;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/*
 * 2018-07-11 KDH
 */

public class MainActivity extends AppCompatActivity {
    private long lastTimeBackPressed; // 뒤로가기 버튼 두 번 사이의 간격 체크 변수
    private Bluetooth bluetoothService = null;
    final String url = "http://14.63.161.0:3000/";

    // Siren을 위한 변수들
    boolean chkSiren = false;
    int streamSiren;
    // Flash를 위한 변수들
    boolean chkFlash = false;
    private Camera camera;
    //GPS를 위한 변수들
    private final int PERMISSIONS_ACCESS_FINE_LOCATION = 1000;
    private final int PERMISSIONS_ACCESS_COARSE_LOCATION = 1001;
    private boolean isAccessFineLocation = false;
    private boolean isAccessCoarseLocation = false;
    private boolean isPermission = false;
    private GpsInfo gps;
    TextView locationtest;
    TextView nametest;
    TextView ecphonetest;
    //문자 보내기를 위한 변수
    SmsManager mSMSManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*-------Button 정의-----------*/
        Button connBluetoothButton= (Button) findViewById(R.id.bluetoothButton);
        Button sirenButton = (Button) findViewById(R.id.sirenButton);
        Button flashButton = (Button) findViewById(R.id.flashButton);
        Button userinfoButton = (Button) findViewById(R.id.userinfoButton);
        Button emergencyButton = (Button) findViewById(R.id.emergencyButton);

        locationtest = (TextView) findViewById(R.id.locationtest);
        nametest = (TextView) findViewById(R.id.nametest);
        ecphonetest = (TextView) findViewById(R.id.ecphonetest);

        //Bluetooth 객체 생성 및 블루투스
        if (bluetoothService == null) {
            bluetoothService = new Bluetooth(this, new Handler() {
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                }
            });
        }
        // siren을 위한 Soundpoop 객체 생성
        final SoundPool sirenPool = new SoundPool(1, AudioManager.STREAM_ALARM, 0);// maxStreams, streamType, srcQuality
        final int sirenSound = sirenPool.load(this, R.raw.siren, 1); // Siren을 load
        //flash를 위한 camera 처리
        //카메라 권한 묻기
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 50);
        //UUID를 위한 폰 상태 접근 권한 묻기
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 50);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 50);


        camera = Camera.open();
        /*------사용자 설정 버튼을 누르면 사용자 설정 화면으로 이동-------*/
        userinfoButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), UserinfoActivity.class);
                intent.putExtra("url", url);
                startActivity(intent);
            }
        });
        /*------블루투스 버튼을 누르면 켜고 꺼짐--------*/
        connBluetoothButton.setOnClickListener(bluetoothService.mClickListener);

        /*------사이렌 버튼을 누르면 켜고 꺼짐---------*/
        sirenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!chkSiren) { // 사이렌이 꺼져있을 경우 켠다
                    chkSiren=true;
                    streamSiren = sirenPool.play(sirenSound, 1.0F, 1.0F,  1,  -1,  1.0F);
                }
                else { // 사이렌이 켜져있을 경우 끈다.
                    chkSiren=false;
                    sirenPool.stop(streamSiren);
                }
            }
        });
        /*-----플래시 버튼을 누르면 켜고 꺼짐---------*/
        flashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!chkFlash) {
                    chkFlash=true;
                    Camera.Parameters param = camera.getParameters();
                    param.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    camera.setParameters(param);
                    camera.startPreview();
                }
                else {
                    chkFlash=false;
                    Camera.Parameters param = camera.getParameters();
                    param.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    camera.setParameters(param);
                    camera.startPreview();
                }
            }
        });
        /*-------버튼을 누를 경우 GPS 위도 경도 받아와 주소로 변환 후 서버에 name과 ecPhone 요청 후 SMS 전송--------*/
        emergencyButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View arg0) {
                String location="";
                // 권한 요청을 해야 함
                if (!isPermission) {
                    callPermission();
                    return;
                }

                gps = new GpsInfo(MainActivity.this);
                // GPS 사용유무 가져오기
                if (gps.isGetLocation()) {
                    // 위도, 경도 구하기
                    double latitude = gps.getLatitude();
                    double longitude = gps.getLongitude();

                    // 구한 위도, 경도를 이용해 주소 구하기
                    location = getAddress(MainActivity.this,latitude,longitude);

                    locationtest.setText(location);

                    //Toast.makeTex(getApplicationContext(),"당신의 위치 - " + location,Toast.LENGTH_LONG).show();
                } else {
                    // GPS 를 사용할수 없으므로
                    gps.showSettingsAlert();
                }
                /*-----UUID 가져오기-----*/
                String uuid = GetDevicesUUID(getApplicationContext());
                Log.e("uuid", uuid);
                /*-----통신을 위한 AsyncTask-----*/
                emergencyTask emergencytask = new emergencyTask(url, uuid, location);
                emergencytask.execute();
                Log.e("tag","execute");
            }
        });
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(chkFlash) {
            camera.release();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /*--------뒤로가기 두 번 누르면 앱 종료----------*/
    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - lastTimeBackPressed < 1500) {
            finish();
            return;
        }
        Toast.makeText(this, "'뒤로' 버튼을 한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
        lastTimeBackPressed = System.currentTimeMillis();
    }
    /*----GPS 권한이 있는지 확인----*/
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSIONS_ACCESS_FINE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            isAccessFineLocation = true;

        } else if (requestCode == PERMISSIONS_ACCESS_COARSE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED){

            isAccessCoarseLocation = true;
        }

        if (isAccessFineLocation && isAccessCoarseLocation) {
            isPermission = true;
        }
    }

    // GPS 권한 요청
    private void callPermission() {
        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_ACCESS_FINE_LOCATION);

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_ACCESS_COARSE_LOCATION);
        } else {
            isPermission = true;
        }
    }
    /**
     * 위도,경도로 주소구하기
     * @param lat
     * @param lng
     * @return 주소
     */
    public static String getAddress(Context mContext, double lat, double lng) {
        String nowAddress ="현재 위치를 확인 할 수 없습니다.";
        Geocoder geocoder = new Geocoder(mContext, Locale.KOREA);
        List<Address> address;
        try {
            if (geocoder != null) {
                //세번째 파라미터는 좌표에 대해 주소를 리턴 받는 갯수로
                //한좌표에 대해 두개이상의 이름이 존재할수있기에 주소배열을 리턴받기 위해 최대갯수 설정
                address = geocoder.getFromLocation(lat, lng, 1);

                if (address != null && address.size() > 0) {
                    // 주소 받아오기
                    String currentLocationAddress = address.get(0).getAddressLine(0).toString();
                    nowAddress  = currentLocationAddress;

                }
            }

        } catch (IOException e) {
            Toast.makeText(mContext, "주소를 가져 올 수 없습니다.", Toast.LENGTH_LONG).show();

            e.printStackTrace();
        }
        return nowAddress;
    }
    /*-----name과 ecphone을 받아오기 위한 AsyncTask----*/
    class emergencyTask extends AsyncTask<String,Integer, JSONObject> {
        String url;
        String uuid;
        String username;
        String ecPhone;
        String location;
        /*------생성자------*/
        public emergencyTask(String url, String uuid, String location) {
            this.url = url+"emergency";
            this.uuid = uuid;
            this.location=location;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        /*-----JSON화 후 통신-------*/
        @Override
        protected JSONObject doInBackground(String... params) {
            Log.e("url", url);
            JSONObject result;
            JSONObject jsonObject = new JSONObject();
            try{
                jsonObject.put("uuid", uuid);
            }catch (JSONException e){
                e.printStackTrace();
            }
            RequestHttpURLConnection requestHttpURLConnection = new RequestHttpURLConnection();
            result = requestHttpURLConnection.request(url, jsonObject, "POST");
            Log.e("Async","Async");
            //Log.e("result",result.toString());
            return result;
        }
        /*----통신의 결과값을 이용하여 문자 전송----*/
        @Override
        protected void onPostExecute(JSONObject result) {
            super.onPostExecute(result);
            try {
                ecPhone = result.getString("ecPhone");
                username = result.getString("name");
            } catch(JSONException e) {
                e.printStackTrace();
            }
            sendSms(ecPhone, username, location);
        }
    }
    /*----UUID 받아오기----*/
    public String GetDevicesUUID(Context mContext) {
        final TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        final String tmDevice, tmSerial, androidId;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            tmDevice = "" + tm.getDeviceId();
            tmSerial = "" + tm.getSimSerialNumber();
            androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
            String deviceId = deviceUuid.toString();
            return deviceId;
        }
        else
            return "fail";
    }

    public void sendSms(String ecPhone, String username, String location){
        mSMSManager = SmsManager.getDefault();
        //메시지
        String smsText = username+"님의 긴급 요청입니다. 위치는 " + location + "입니다. 긴급전화 119";
        Log.e("body", smsText);
        //송신 인텐트
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent("SMS_SENT"), 0);
        //수신 인텐트
        PendingIntent recvPI = PendingIntent.getBroadcast(this, 0, new Intent("SMS_DELIVERED"), 0);

        registerReceiver(mSentReceiver, new IntentFilter("SMS_SENT"));
        registerReceiver(mRecvReceiver, new IntentFilter("SMS_DELIVERED"));


        mSMSManager.sendTextMessage(ecPhone, null, smsText, sentPI, recvPI);

    }
    BroadcastReceiver mSentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (getResultCode()){
                case RESULT_OK:
                    Toast.makeText(MainActivity.this, "SMS Send", Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    Toast.makeText(MainActivity.this, "ERROR_GENERIC_FAILURE", Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    Toast.makeText(MainActivity.this, "ERROR_NO_SERVICE", Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    Toast.makeText(MainActivity.this, "ERROR_NULL_PDU", Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    Toast.makeText(MainActivity.this, "ERROR_RADIO_OFF", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    BroadcastReceiver mRecvReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (getResultCode()){
                case RESULT_OK:
                    Toast.makeText(MainActivity.this, "SMS Delivered", Toast.LENGTH_SHORT).show();
                    break;
                case RESULT_CANCELED:
                    Toast.makeText(MainActivity.this, "SMS Delivered Fail", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
}
