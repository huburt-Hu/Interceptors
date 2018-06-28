package com.huburt.interceptors;

import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSource;

/**
 * Created by Yune on 2017/4/25.
 */

public class LogInterceptor implements Interceptor {

    private static final String HTTP_LOG = "http_log";

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        long t1 = System.nanoTime();

        Buffer buffer = new Buffer();
        RequestBody body = request.body();
        if (body != null)
            body.writeTo(buffer);
        Log.e(HTTP_LOG, String.format("Sending request %s on %s %n%s Request Params: %s",
                request.url(), chain.connection(), request.headers(), buffer.clone().readUtf8()));
        buffer.close();

        Response response = chain.proceed(request);
        long t2 = System.nanoTime();

        BufferedSource source = response.body().source();
        source.request(Long.MAX_VALUE);
        buffer = source.buffer().clone();
        Log.e(HTTP_LOG, String.format("Received response for %s in %.1fms \ncode:%s %n%s Response Json: %s",
                response.request().url(), (t2 - t1) / 1e6d, response.code(), response.headers(),
                buffer.readUtf8()));
        return response;
    }
}
