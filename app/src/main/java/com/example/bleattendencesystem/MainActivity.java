package com.example.bleattendencesystem;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private TextView statView;
    private EditText IDInsert;
    private EditText PWInsert;
    private Button loginButton;

    private String mJsonString;

    private Button membership;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statView = (TextView)findViewById(R.id.stat);
        IDInsert = (EditText)findViewById(R.id.ID);
        PWInsert = (EditText)findViewById(R.id.PW);
        loginButton = (Button)findViewById(R.id.login);
        membership = (Button)findViewById(R.id.membership);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(IDInsert.getText().toString().isEmpty() || PWInsert.getText().toString().isEmpty()){
                    Toast.makeText(getApplicationContext(), String.format("ID와 PW 모두 입력해주세요"), Toast.LENGTH_SHORT).show();
                }
                else {
                    LoginProcess task = new LoginProcess();
                    task.execute(IDInsert.getText().toString(), PWInsert.getText().toString());
                }
            }
        });

        membership.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), String.format("점검중입니다."), Toast.LENGTH_SHORT).show();
                /*Intent intent = new Intent(MainActivity.this, SubActivity.class);
                startActivity(intent);*/
            }
        });
    }

    private class LoginProcess extends AsyncTask<String, Void, String>{
        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(MainActivity.this, "Please Wait", null, true, true);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressDialog.dismiss();

            if (result == null){
                statView.setText(errorString);
            }
            else {
                mJsonString = result;
                if(mJsonString.equals("잘못된 ID 혹은 PW입니다.") || mJsonString.equals("ID와 PW를 모두 입력해주세요")){
                    statView.setText(mJsonString);
                }
                else {
                    try {
                        JSONObject jsonObject = new JSONObject(mJsonString);
                        JSONArray jsonArray = jsonObject.getJSONArray("login data");

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject item = jsonArray.getJSONObject(i);

                            int id = item.getInt("id");
                            String name = item.getString("name");
                            String role = item.getString("role");

                            statView.setText(role + "로 로그인하였습니다.");
                            if(role.equals("student")) {
                                Intent intent = new Intent(MainActivity.this, StudentActivity.class);
                                intent.putExtra("id", id);
                                intent.putExtra("name", name);
                                startActivity(intent);
                            }
                            else{
                                Intent intent = new Intent(MainActivity.this, ProfessorActivity.class);
                                intent.putExtra("id", id);
                                intent.putExtra("name", name);
                                startActivity(intent);
                            }
                            finish();
                        }
                    } catch (JSONException e) {
                        statView.setText(e.toString());
                    }
                }
            }
        }

        @Override
        protected String doInBackground(String... params) {
            String searchKeyword1 = params[0];
            String searchKeyword2 = params[1];

            String serverURL = "http://172.30.1.24/login.php";
            String postParameters = "id=" + searchKeyword1 + "&pw=" + searchKeyword2;

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
                errorString = e.toString();
                return null;
            }
        }
    }
}
