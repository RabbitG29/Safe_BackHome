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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPOIItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;

public class MapActivity extends Activity  {
    private GpsInfo gps;
    private WebView safemapView;
    private WebSettings safemapSettings;
    final static String safeurl="http://14.63.161.0:3000/safemap";
    final static int A_ACTIVITY_RESULT = 1;
    EditText edit1;
    final static int B_ACTIVITY_RESULT = 2;
    EditText edit2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.setContentView(R.layout.activity_map);
        /*----TMap 관련 변수 정의----*/
        final LinearLayout linearLayoutTmap = (LinearLayout)findViewById(R.id.linearLayoutTmap);
        final TMapView tMapView = new TMapView(this);
        // 마커 아이콘
        final Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.pin);
        final Bitmap bitmap2 = BitmapFactory.decodeResource(this.getResources(), R.drawable.pin2);
        final Bitmap bitmap3 = BitmapFactory.decodeResource(this.getResources(), R.drawable.pin3);

        tMapView.setSKTMapApiKey( "ef7447fe-4766-4011-902b-a8117a302dd8" );
        location(tMapView);
        /*----생활안전지도 관련 Webview 변수 정의----*/
        safemapView = (WebView) findViewById(R.id.safeMap);
        safemapSettings = safemapView.getSettings();
        safemapSettings.setSupportZoom(true);
        safemapSettings.setDomStorageEnabled(true);
        safemapSettings.setLoadWithOverviewMode(true);
        safemapSettings.setUseWideViewPort(true);
        safemapSettings.setJavaScriptEnabled(true);
        safemapView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String safeurl) {
                view.loadUrl(safeurl);
                return true;
            }
        });
        safemapView.setWebChromeClient(new WebChromeClient(){
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.e("시발", consoleMessage.message() + '\n' + consoleMessage.messageLevel() + '\n' + consoleMessage.sourceId());
                return super.onConsoleMessage(consoleMessage);
            }
        });
        Log.e("execute","execute");
        safemapView.loadUrl(safeurl);

        Button pathButton = (Button) findViewById(R.id.pathButton);
        Button locationButton = (Button) findViewById(R.id.locationButton);

        // GPS 사용유무 가져오기


        /*-----길찾기 버튼 누를 시-----*/
        pathButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                LinearLayout pathLayout = (LinearLayout) vi.inflate(R.layout.path_layout, null);
                final String[] path = new String[]{"인하대학교", "SKT타워"}; // 기본 초기화
                Button pathButton1 = (Button) pathLayout.findViewById(R.id.button1); // 출발지
                edit1 = (EditText) pathLayout.findViewById(R.id.edit1);
                pathButton1.setOnClickListener(new View.OnClickListener() { // 출발지
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getApplicationContext(), DaumWebViewActivity.class);
                        startActivityForResult(intent, A_ACTIVITY_RESULT); // 출발지 신호는 A
                        intent = getIntent();
                        onActivityResult(A_ACTIVITY_RESULT, RESULT_OK, intent);
                    }
                });
                Button pathButton2 = (Button) pathLayout.findViewById(R.id.button2); // 도착지
                edit2 = (EditText) pathLayout.findViewById(R.id.edit2);
                pathButton2.setOnClickListener(new View.OnClickListener() { // 도착지
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getApplicationContext(), DaumWebViewActivity.class);
                        startActivityForResult(intent, B_ACTIVITY_RESULT); // 도착지 신호는 B
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
        /*----현위치 버튼 눌렀을 경우----*/
        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                location(tMapView);
            }
        });
        linearLayoutTmap.addView( tMapView );
        super.onCreate(savedInstanceState);
    }
    /*----주소를 위경도로 변환----*/
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
    /*----주소 검색 이후 intent 정보 받아오기----*/
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

    void location(final TMapView tMapView) {
        /*----GPS 사용----*/
        gps = new GpsInfo(MapActivity.this);
        final Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.pin);
        final Bitmap bitmap2 = BitmapFactory.decodeResource(this.getResources(), R.drawable.pin2);
        final Bitmap bitmap3 = BitmapFactory.decodeResource(this.getResources(), R.drawable.pin3);
        TMapMarkerItem markerItem1 = new TMapMarkerItem();
        TMapPoint tMapPoint1;
        TMapPOIItem tMapPOIItem = new TMapPOIItem();
        TMapData tmapdata = new TMapData();
        if (gps.isGetLocation()) {
            // 위도, 경도 구하기
            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();
            tMapView.setCenterPoint(longitude, latitude);
            tMapPoint1 = new TMapPoint(latitude, longitude);
            final TMapMarkerItem POIitem[] = new TMapMarkerItem[200];
            tmapdata.findAroundNamePOI(tMapPoint1, "관공서;편의점;", new TMapData.FindAroundNamePOIListenerCallback() {
                @Override
                public void onFindAroundNamePOI(ArrayList<TMapPOIItem> poiItem) {
                    if(poiItem==null)
                        return;
                    for(int i = 0; i < poiItem.size(); i++) {
                        TMapPOIItem  item = poiItem.get(i);
                        Log.e("Hi","POI Name: " + item.getPOIName().toString() + ", " +
                                "Address: " + item.getPOIAddress().replace("null", "")  + ", " +
                                "Point: " + item.getPOIPoint().toString());
                        if(item.getPOIName().toString()=="편의점") {
                            POIitem[i] = new TMapMarkerItem();
                            POIitem[i].setIcon(bitmap2);
                            POIitem[i].setPosition(0.5f, 1.0f); // 마커의 중심점을 중앙, 하단으로 설정
                            POIitem[i].setTMapPoint( item.getPOIPoint() ); // 마커의 좌표 지정
                            POIitem[i].setName(item.getPOIName().toString());
                            tMapView.addMarkerItem(item.getPOIID(), POIitem[i]);
                        }
                        else {
                            POIitem[i] = new TMapMarkerItem();
                            POIitem[i].setIcon(bitmap3);
                            POIitem[i].setPosition(0.5f, 1.0f); // 마커의 중심점을 중앙, 하단으로 설정
                            POIitem[i].setTMapPoint( item.getPOIPoint() ); // 마커의 좌표 지정
                            POIitem[i].setName(item.getPOIName().toString());
                            tMapView.addMarkerItem(item.getPOIID(), POIitem[i]);
                        }
                    }
                }
            });
        } else {
            // GPS 를 사용할수 없으므로
            gps.showSettingsAlert();
            tMapView.setCenterPoint(126.988205, 37.551135); // 서울N타워
            tMapPoint1 = new TMapPoint(37.551135, 126.988205);
        }

        markerItem1.setIcon(bitmap); // 마커 아이콘 지정
        markerItem1.setPosition(0.5f, 1.0f); // 마커의 중심점을 중앙, 하단으로 설정
        markerItem1.setTMapPoint( tMapPoint1 ); // 마커의 좌표 지정
        markerItem1.setName("현재위치"); // 마커의 타이틀 지정
        tMapView.addMarkerItem("markerItem1", markerItem1); // 지도에 마커 추가
    }
}
