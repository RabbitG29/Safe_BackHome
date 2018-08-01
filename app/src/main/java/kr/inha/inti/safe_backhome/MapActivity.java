package kr.inha.inti.safe_backhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;

import java.io.IOException;
import java.util.List;

public class MapActivity extends Activity  {
    private GpsInfo gps;
    final static int A_ACTIVITY_RESULT = 1;
    EditText edit1;
    final static int B_ACTIVITY_RESULT = 2;
    EditText edit2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.setContentView(R.layout.activity_map);
        final LinearLayout linearLayoutTmap = (LinearLayout)findViewById(R.id.linearLayoutTmap);
        final TMapView tMapView = new TMapView(this);

        tMapView.setSKTMapApiKey( "ef7447fe-4766-4011-902b-a8117a302dd8" );
        TMapMarkerItem markerItem1 = new TMapMarkerItem();

        TMapPoint tMapPoint1;

        Button pathButton = (Button) findViewById(R.id.pathButton);

        gps = new GpsInfo(MapActivity.this);
        // GPS 사용유무 가져오기
        if (gps.isGetLocation()) {
            // 위도, 경도 구하기
            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();
            tMapView.setCenterPoint(longitude, latitude);
            tMapPoint1 = new TMapPoint(latitude, longitude);
            //Toast.makeTex(getApplicationContext(),"당신의 위치 - " + location,Toast.LENGTH_LONG).show();
        } else {
            // GPS 를 사용할수 없으므로
            gps.showSettingsAlert();
            tMapView.setCenterPoint(126.988205, 37.551135); // 서울N타워
            tMapPoint1 = new TMapPoint(37.551135, 126.988205);
        }
        // 마커 아이콘
        final Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.pin);
        markerItem1.setIcon(bitmap); // 마커 아이콘 지정
        markerItem1.setPosition(0.5f, 1.0f); // 마커의 중심점을 중앙, 하단으로 설정
        markerItem1.setTMapPoint( tMapPoint1 ); // 마커의 좌표 지정
        markerItem1.setName("현재위치"); // 마커의 타이틀 지정
        tMapView.addMarkerItem("markerItem1", markerItem1); // 지도에 마커 추가
        pathButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                LinearLayout pathLayout = (LinearLayout) vi.inflate(R.layout.path_layout, null);
                final String[] path = new String[]{"인하대학교", "SKT타워"}; // 기본 초기화
                Button pathButton1 = (Button) pathLayout.findViewById(R.id.button1);
                edit1 = (EditText) pathLayout.findViewById(R.id.edit1);
                pathButton1.setOnClickListener(new View.OnClickListener() { // 출발지
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getApplicationContext(), DaumWebViewActivity.class);
                        startActivityForResult(intent, A_ACTIVITY_RESULT);
                        intent = getIntent();
                        onActivityResult(A_ACTIVITY_RESULT, RESULT_OK, intent);
                    }
                });
                Button pathButton2 = (Button) pathLayout.findViewById(R.id.button2);
                edit2 = (EditText) pathLayout.findViewById(R.id.edit2);
                pathButton2.setOnClickListener(new View.OnClickListener() { // 도착지
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getApplicationContext(), DaumWebViewActivity.class);
                        startActivityForResult(intent, B_ACTIVITY_RESULT);
                        intent = getIntent();
                        onActivityResult(B_ACTIVITY_RESULT, RESULT_OK, intent);
                    }
                });
                AlertDialog.Builder pathDialog = new AlertDialog.Builder(MapActivity.this);
                pathDialog.setTitle("길찾기");
                pathDialog.setView(pathLayout);
                pathDialog.setPositiveButton("입력",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                path[0]=edit1.getText().toString();
                                path[1]=edit2.getText().toString();
                                pathTask path1 = new pathTask(path[0], path[1], tMapView, bitmap);
                                path1.execute();
                            }
                        });
                pathDialog.create().show();
            }
        });
        linearLayoutTmap.addView( tMapView );
        super.onCreate(savedInstanceState);
    }
    public static Location findGeoPoint(Context mcontext, String address) {
        Location loc = new Location("");
        Geocoder coder = new Geocoder(mcontext);
        List<Address> addr = null;// 한좌표에 대해 두개이상의 이름이 존재할수있기에 주소배열을 리턴받기 위해 설정

        try {
            addr = coder.getFromLocationName(address, 5);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }// 몇개 까지의 주소를 원하는지 지정 1~5개 정도가 적당
        if (addr != null) {
            for (int i = 0; i < addr.size(); i++) {
                Address lating = addr.get(i);
                double lat = lating.getLatitude(); // 위도가져오기
                double lon = lating.getLongitude(); // 경도가져오기
                loc.setLatitude(lat);
                loc.setLongitude(lon);
            }
        }
        return loc;
    }
    /*---길찾기 AsyncTask----*/
    class pathTask extends AsyncTask<String, String, TMapPolyLine> {
        String start, end; // 시작지점과 끝지점 주소로 받아옴
        TMapView tMapView;
        Bitmap bitmap;
        TMapPoint tMapPointStart, tMapPointEnd;
        public pathTask(String start, String end, TMapView tMapView, Bitmap bitmap) {
            this.start = start;
            this.end = end;
            this.tMapView = tMapView;
            this.bitmap = bitmap;
        }
        @Override
        protected TMapPolyLine doInBackground(String... params) {
            /*----받은 주소를 위,경도로 변환----*/
            tMapPointStart = new TMapPoint(findGeoPoint(MapActivity.this, start).getLatitude(), findGeoPoint(MapActivity.this, start).getLongitude()); // SKT타워(출발지)
            tMapPointEnd = new TMapPoint(findGeoPoint(MapActivity.this, end).getLatitude(), findGeoPoint(MapActivity.this, end).getLongitude());
            try{
                TMapPolyLine tMapPolyLine = new TMapData().findPathDataWithType(TMapData.TMapPathType.PEDESTRIAN_PATH,tMapPointStart, tMapPointEnd); // 보행자 경로 찾기
                return tMapPolyLine;
            }catch(Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        protected void onPostExecute(TMapPolyLine tMapPolyLine) {
            /*----UI 관련 작업 Post에서 처리----*/
            tMapPolyLine.setLineColor(Color.BLUE);
            tMapPolyLine.setLineWidth(2);
            tMapView.addTMapPolyLine("Line1", tMapPolyLine);
            TMapMarkerItem markerItemStart = new TMapMarkerItem();
            TMapMarkerItem markerItemEnd = new TMapMarkerItem();
            markerItemStart.setIcon(bitmap); // 마커 아이콘 지정
            markerItemStart.setPosition(0.5f, 1.0f); // 마커의 중심점을 중앙, 하단으로 설정
            markerItemStart.setTMapPoint( tMapPointStart ); // 마커의 좌표 지정
            markerItemStart.setName("시작위치"); // 마커의 타이틀 지정
            markerItemEnd.setIcon(bitmap); // 마커 아이콘 지정
            markerItemEnd.setPosition(0.5f, 1.0f); // 마커의 중심점을 중앙, 하단으로 설정
            markerItemEnd.setTMapPoint( tMapPointEnd ); // 마커의 좌표 지정
            markerItemEnd.setName("도착위치"); // 마커의 타이틀 지정
            tMapView.addMarkerItem("markerItemStart", markerItemStart); // 지도에 마커 추가
            tMapView.addMarkerItem("markerItemEnd", markerItemEnd); // 지도에 마커 추가
            tMapView.setCenterPoint(tMapPointStart.getLongitude(),tMapPointStart.getLatitude());
        }
    }
    @Override
    protected void onActivityResult(int requestcode, int resultcode, Intent data) {
        super.onActivityResult(requestcode, resultcode, data);
        if(resultcode==RESULT_OK) {
            switch(requestcode) {
                case A_ACTIVITY_RESULT: {
                    edit1.setText(data.getStringExtra("path")); // 출발지 검색 버튼일 경우
                } break;
                case B_ACTIVITY_RESULT: {
                    edit2.setText(data.getStringExtra("path")); // 도착지 검색 버튼일 경우
                } break;
            }
        }
    }
}
