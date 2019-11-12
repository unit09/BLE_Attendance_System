package com.example.bleattendencesystem;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

public class StartAttendanceActivity extends AppCompatActivity implements BeaconConsumer {
    private int lid;
    private BeaconManager beaconManager;
    private ArrayList<Beacon> beaconList;

    private TextView textView;
    private ListView listView;
    private ArrayList<String> listMenu;

    private Button search;
    private Button start;
    private Button end;

    private int select = -1;
    private String mJsonString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        Intent passStart = getIntent();
        lid = passStart.getIntExtra("lid", 0);

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconList = new ArrayList<Beacon>();

        textView = (TextView)findViewById(R.id.textView);
        listView = (ListView)findViewById(R.id.list);
        listMenu = new ArrayList<String>();

        search = (Button)findViewById(R.id.button1);
        search.setOnClickListener(new searchBeacon());
        start = (Button)findViewById(R.id.button2);
        start.setOnClickListener(new startAttendance());
        end = (Button)findViewById(R.id.button3);
        end.setOnClickListener(new endAttendance());

        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        beaconManager.bind(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                beaconList.clear();
                if (beacons.size() > 0) {
                    for (Beacon beacon : beacons) {
                        beaconList.add(beacon);
                    }
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    class searchBeacon implements View.OnClickListener {    //그럭저럭 괜찮긴하지만 검색하는 간격에 걸리면 제대로 안뜸
        public void onClick(View v) {
            textView.setText("비콘 검색 완료");
            listMenu.clear();

            for(Beacon beacon : beaconList){
                listMenu.add("UID : " + beacon.getId1() + "\nID : " + beacon.getId2() + " / Distance : " + beacon.getDistance() + "m\n");
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(StartAttendanceActivity.this, android.R.layout.simple_list_item_1, listMenu);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView parent, View v, int position, long id) {
                    int index = (int)parent.getItemIdAtPosition(position);
                    select = beaconList.get(index).getId2().toInt();
                    Toast.makeText(getApplicationContext(), String.format("비콘 ID = %d 가 선택됨", select), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    class startAttendance implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if(select == -1){
                Toast.makeText(getApplicationContext(), "비콘을 선택해주세요", Toast.LENGTH_SHORT).show();
            }
            else {
                //이거 여러개의 비콘으로 출석할 수 있게 해야하나 말아야하나?
                CheckBeacon checkTask = new CheckBeacon();
                checkTask.execute(Integer.toString(select));
            }
        }
    }

    class endAttendance implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if(select == -1){
                Toast.makeText(getApplicationContext(), "비콘을 선택해주세요", Toast.LENGTH_SHORT).show();
            }
            else{
                //checkBeacon 수정해서 자신의 강의만 종료할 수 있게 앱 단계에서 처리하는 걸로 수정
                UpdateBeacon endTask = new UpdateBeacon();
                endTask.execute(Integer.toString(select), Integer.toString(lid), "2");
            }
        }
    }

    private class CheckBeacon extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(StartAttendanceActivity.this, "Please Wait", null, true, true);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressDialog.dismiss();

            if (result == null){
                textView.setText(errorString);
            }
            else {
                mJsonString = result;
                if(mJsonString.equals("비콘이 존재하지 않습니다.") || mJsonString.equals("정보를 받아오는데 실패했습니다.")){
                    textView.setText(mJsonString);
                }
                else {
                    try {
                        JSONObject jsonObject = new JSONObject(mJsonString);
                        JSONArray jsonArray = jsonObject.getJSONArray("lecture data");

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject item = jsonArray.getJSONObject(i);

                            int lidCheck = item.getInt("lid");
                            int state = item.getInt("state");

                            if(lidCheck == 0){
                                if(state == 0){
                                    UpdateBeacon updateTask = new UpdateBeacon();
                                    updateTask.execute(Integer.toString(select), Integer.toString(lid), "1");
                                }
                                else{
                                    Toast.makeText(getApplicationContext(), "이미 출석이 진행중입니다", Toast.LENGTH_SHORT).show();
                                }
                            }
                            else{
                                Toast.makeText(getApplicationContext(), "이미 사용중인 비콘입니다", Toast.LENGTH_SHORT).show();
                            }

                        }
                    } catch (JSONException e) {
                        textView.setText("onPost" + e.toString());
                    }
                }
            }
        }

        @Override
        protected String doInBackground(String... params) {
            String searchKeyword1 = params[0];

            String serverURL = "http://172.30.1.24/checkbeacon.php";
            String postParameters = "id=" + searchKeyword1;

            try {
                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();

                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();

                int responseStatusCode = httpURLConnection.getResponseCode();

                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                }
                else{
                    inputStream = httpURLConnection.getErrorStream();
                }

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line;

                while((line = bufferedReader.readLine()) != null){
                    sb.append(line);
                }

                bufferedReader.close();

                return sb.toString().trim();
            }
            catch (Exception e) {
                errorString = "do : " + e.toString();
                return null;
            }
        }
    }

    private class UpdateBeacon extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(StartAttendanceActivity.this, "Please Wait", null, true, true);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
            textView.setText(result);
        }

        @Override
        protected String doInBackground(String... params) {
            String id = params[0];
            String lid = params[1];
            String mode = params[2];

            String serverURL = "http://172.30.1.24/updatebeacon.php";
            String postParameters = "id=" + id + "&lid=" + lid + "&mode=" + mode;

            try {
                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.connect();

                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();

                int responseStatusCode = httpURLConnection.getResponseCode();

                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                }
                else{
                    inputStream = httpURLConnection.getErrorStream();
                }

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line = null;

                while((line = bufferedReader.readLine()) != null){
                    sb.append(line);
                }

                bufferedReader.close();

                return sb.toString().trim();
            }
            catch (Exception e) {
                errorString = "do : " + e.toString();
                return errorString;
            }
        }
    }
}
