package com.example.bleattendencesystem;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.altbeacon.beacon.*;
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
import java.util.List;

public class ProfessorActivity extends AppCompatActivity {
    private TextView textView;
    private ListView listView;
    private ArrayList<String> lectureDetail;
    private ArrayList<Integer> lectureID;   // 나중에 출석할때 바로 조회하기위한 용도
    private int professorID;
    private String professorName;

    private String mJsonString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student);

        listView = (ListView)findViewById(R.id.list);
        lectureDetail = new ArrayList<String>();
        lectureID = new ArrayList<Integer>();

        Intent intent = getIntent();
        professorID = intent.getIntExtra("id", 0);
        professorName = intent.getStringExtra("name");

        textView = (TextView)findViewById(R.id.text);
        textView.setText("안녕하세요. " + professorName + "님.");

        GetLecture listTask = new GetLecture();
        listTask.execute(Integer.toString(professorID));


        if(lectureID.isEmpty()) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, lectureDetail);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView parent, View v, int position, long id) {
                    int index = (int) parent.getItemIdAtPosition(position);
                    Intent passStart = new Intent(ProfessorActivity.this, StartAttendanceActivity.class);
                    passStart.putExtra("lid", lectureID.get(index));
                    startActivity(passStart);
                }
            });
        }

    }

    private class GetLecture extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(ProfessorActivity.this, "Please Wait", null, true, true);
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
                if(mJsonString.equals("수업이 없습니다.") || mJsonString.equals("정보를 받아오는데 실패했습니다.")){
                    textView.setText(mJsonString);
                }
                else {
                    try {
                        JSONObject jsonObject = new JSONObject(mJsonString);
                        JSONArray jsonArray = jsonObject.getJSONArray("lecture data");

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject item = jsonArray.getJSONObject(i);

                            int lid = item.getInt("lid");
                            String lname = item.getString("lname");
                            String time = item.getString("time");

                            lectureDetail.add(lname +  "\n수업 시간 : " + time);
                            lectureID.add(lid);

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

            String serverURL = "http://172.30.1.24/getlecture.php";
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
}

