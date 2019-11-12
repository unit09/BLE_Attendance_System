package com.example.bleattendencesystem;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SubActivity extends AppCompatActivity {
    private TextView statView;
    private EditText IDInsert;
    private EditText PWInsert;
    private EditText RoleInsert;
    private Button membership;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub);

        statView = (TextView)findViewById(R.id.stat);
        IDInsert = (EditText)findViewById(R.id.ID);
        PWInsert = (EditText)findViewById(R.id.PW);
        RoleInsert = (EditText)findViewById(R.id.Role);
        membership = (Button)findViewById(R.id.membership);

        membership.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InsertData task = new InsertData();
                task.execute("http://220.66.218.122/insert.php", IDInsert.getText().toString(), PWInsert.getText().toString(), RoleInsert.getText().toString());

                IDInsert.setText("");
                PWInsert.setText("");
                RoleInsert.setText("");
            }
        });
    }

    class InsertData extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(SubActivity.this, "Please Wait", null, true, true);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
            statView.setText(result);
        }

        @Override
        protected String doInBackground(String... params) {
            String id = (String)params[1];
            String pw = (String)params[2];
            String role = (String)params[3];

            String serverURL = (String)params[0];
            String postParameters = "id=" + id + "&pw=" + pw + "&role=" + role;

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

                return sb.toString();
            }
            catch (Exception e) {
                return new String("Error: " + e.getMessage());
            }
        }
    }
}
