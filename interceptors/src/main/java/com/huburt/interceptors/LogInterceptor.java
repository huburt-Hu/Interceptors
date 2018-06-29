package com.huburt.interceptors;

import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSource;


public class LogInterceptor implements Interceptor {

    private static final String HTTP_LOG = "http_log";

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        printRequest(request);
        long t1 = System.nanoTime();
        Response response = chain.proceed(request);
        long t2 = System.nanoTime();
        double takeTime = (t2 - t1) / 1e6d;
        printResponse(response, takeTime);
        return response;
    }

    protected void printResponse(Response response, double takeTime) throws IOException {
        BufferedSource source = response.body().source();
        source.request(Long.MAX_VALUE);
        Buffer buffer = source.buffer().clone();
        Log.e(HTTP_LOG, String.format("Received response for %s in %.1fms \ncode:%s %n%s Response Json: %s",
                response.request().url(), takeTime, response.code(), response.headers(),
                buffer.readUtf8()));
    }

    protected void printRequest(Request request) throws IOException {
        Buffer buffer = new Buffer();
        RequestBody body = request.body();
        if (body != null)
            body.writeTo(buffer);
        Log.e(HTTP_LOG, String.format("Sending request %s  %n%s Request Params: %s",
                request.url(), request.headers(), buffer.clone().readUtf8()));
        buffer.close();
    }
}
