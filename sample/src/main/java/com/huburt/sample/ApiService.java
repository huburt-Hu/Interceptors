package com.huburt.sample;

import com.huburt.interceptors.CacheInterceptor;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;

/**
 * Created by hubert on 2018/6/28.
 */
public interface ApiService {
    @Headers({CacheInterceptor.HEADER_NAME + ":60"})
    @GET("users/{user}/repos")
    Call<ResponseBody> listRepos(@Path("user") String user);
}
