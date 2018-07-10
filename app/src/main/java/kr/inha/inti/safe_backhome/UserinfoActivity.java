package kr.inha.inti.safe_backhome;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;
/*
 *  2018-07-11 KDH
 */

public class UserinfoActivity extends  MainActivity {
    /*---------EditText 선언---------*/
    EditText name;
    EditText ecphoneNumber;
    EditText machine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_userinfo);

        name = (EditText) findViewById(R.id.editname);
        ecphoneNumber = (EditText) findViewById(R.id.editec);
        machine = (EditText) findViewById(R.id.editmachine);
        Button submitButton = (Button) findViewById(R.id.submitButton);
        /*-------마지막 입력 정보 기억---------*/
        SharedPreferences sf = getSharedPreferences("PrefName", MODE_PRIVATE);
        String preName = sf.getString("name","");
        String preMachine = sf.getString("machine","");
        String preEcphone = sf.getString("ecphoneNumber", "");
        name.setText(preName);
        machine.setText(preMachine);
        ecphoneNumber.setText(preEcphone);
        /*----EditText의 String화------*/
        final String Name = name.getText().toString();
        final String EcphoneNumber = ecphoneNumber.getText().toString();
        final String Machine = machine.getText().toString();
        /*-------uuid와 phone number, url 가져오기--------*/
        final String uuid = GetDevicesUUID(this);
        final String myphone = getPhoneNumber();
        Intent intent = getIntent();
        final String url = intent.getStringExtra("url");
        Log.e("tag", url);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UserinfoTask userinfotask = new UserinfoTask(url, uuid, Name, myphone, Machine, EcphoneNumber);
                userinfotask.execute();
                Log.e("tag","execute");
                finish();
            }
        });
    }
    /*-----통신을 위한 AsynkTask 정의------*/
    class UserinfoTask extends AsyncTask<String,Integer, String> {
        String url;
        String uuid, name, myPhone, machineId, ecPhone;
        /*------생성자------*/
        public UserinfoTask(String url, String uuid, String name, String myphone, String machine, String ecphoneNumber) {
            this.url = url;
            this.uuid = uuid;
            this.name = name;
            this.myPhone = myphone;
            this.machineId = machine;
            this.ecPhone = ecphoneNumber;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        /*-----JSON화 후 통신-------*/
        @Override
        protected String doInBackground(String... params) {
            String result = "";
            JSONObject jsonObject = new JSONObject();
            try{
                jsonObject.put("uuid", uuid);
                jsonObject.put("name", name);
                jsonObject.put("myPhone", myPhone);
                jsonObject.put("machineId", machineId);
                jsonObject.put("ecPhone", ecPhone);
            }catch (JSONException e){
                e.printStackTrace();
            }
            RequestHttpURLConnection requestHttpURLConnection = new RequestHttpURLConnection();
            result = requestHttpURLConnection.request(url, jsonObject);
            // 여기도 request 실어 보내는곳 바꿔야함.
            Log.e("Async","Async");
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        /*------SharedPreferences 정보 저장------*/
        SharedPreferences prefs = getSharedPreferences("PrefName", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String tempName = name.getText().toString();
        String tempEcphone = ecphoneNumber.getText().toString();
        String tempMachine = machine.getText().toString();
        editor.putString("name", tempName);
        editor.putString("ecphoneNumber", tempEcphone);
        editor.putString("machine", tempMachine);
        editor.commit();
    }
}
