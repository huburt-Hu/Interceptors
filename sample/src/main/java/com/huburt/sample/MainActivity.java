package com.huburt.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.huburt.interceptors.CacheInterceptor;
import com.huburt.interceptors.LogInterceptor;
import com.huburt.sample.R;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity {

    OkHttpClient client;
    private TextView textView;
    private ApiService service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        client = new OkHttpClient.Builder()
                .addInterceptor(new LogInterceptor())
                .addInterceptor(CacheInterceptor.getDefault(getApplicationContext()))
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .client(client)
                .build();

        service = retrofit.create(ApiService.class);

        textView = findViewById(R.id.tv);
        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CacheInterceptor.getDefault(getApplicationContext())
                        .clearCache();
            }
        });
        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                request();
            }
        });
        findViewById(R.id.btn_retrofit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestByRetrofit();
            }
        });
    }

    private void request() {
        textView.setText("");
        Request request = new Request.Builder()
                .url("http://www.baidu.com")
                .addHeader(CacheInterceptor.HEADER_NAME, String.valueOf(60))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("tag", "onFailure");
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            textView.setText(response.headers().toString() +
                                    "Response:" + response.body().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                });
            }
        });
    }


    private void requestByRetrofit() {
        textView.setText("");
        service.listRepos("huburt-Hu")
                .enqueue(new retrofit2.Callback<ResponseBody>() {
                    @Override
                    public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                        try {
                            textView.setText(response.headers().toString() +
                                    "Response:" + response.body().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {

                    }
                });
    }
}
