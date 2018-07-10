package kr.inha.inti.safe_backhome;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.Menu;
import android.widget.Button;
import android.widget.Toast;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*-------Button 정의-----------*/
        Button connBluetoothButton= (Button) findViewById(R.id.bluetoothButton);
        Button sirenButton = (Button) findViewById(R.id.sirenButton);
        Button flashButton = (Button) findViewById(R.id.flashButton);
        Button userinfoButton = (Button) findViewById(R.id.userinfoButton);

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
        //Phone Number를 위한 폰 번호 접근 권한 묻기
      //  if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED)
         //   ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_NUMBERS}, 50);

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
    /*--------uuid 받아오기----------*/
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
    /*--------phone number 받아오기--------*/
    public String getPhoneNumber() {
        TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String phoneNumber ="";
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            try {
                if (telephony.getLine1Number() != null) {
                    phoneNumber = telephony.getLine1Number();
                }
                else {
                    if (telephony.getSimSerialNumber() != null) {
                        phoneNumber = telephony.getSimSerialNumber();
                    }
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            if(phoneNumber.startsWith("+82")){
                phoneNumber = phoneNumber.replace("+82", "0");
            }
            return phoneNumber;
        }
        else
            return "fail";
    }

}
