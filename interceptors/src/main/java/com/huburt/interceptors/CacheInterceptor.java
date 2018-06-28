package com.huburt.interceptors;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.huburt.interceptors.cache.ACache;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

/**
 * Created by hubert on 2018/6/21.<br>
 * A custom cache interceptor that can cache any request, not just get.<br>
 * usage：<br>
 * 1.add interceptor to the okhttp client;<br>
 * 2.add header 'CacheInterceptor.HEADER_NAME : cacheTime' in the request method that you want to cache.
 */
public abstract class CacheInterceptor<T> implements Interceptor {

    private static final String TAG = "CacheInterceptor";

    public static final String HEADER_NAME = "Cache-custom";

    private static Map<String, CacheInterceptor> interceptors = new HashMap<>();

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String header = request.header(HEADER_NAME);
        if (!TextUtils.isEmpty(header)) {
            String key = getCacheKey(request);
            T cache = getCache(key);
            if (checkUseful(cache)) {
                return createResponseFromCache(request, cache);
            } else {
                Response response = chain.proceed(request);
                if (response.isSuccessful()) {
                    int cacheTime = parseCacheTime(header);
                    saveAsCache(key, convert(response), cacheTime);
                }
                return response;
            }
        }
        return chain.proceed(request);
    }

    private int parseCacheTime(String header) {
        int cacheTime;
        try {
            cacheTime = Integer.parseInt(header);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Cache header with wrong content");
            cacheTime = defaultCacheTime();
        }
        return cacheTime;
    }

    protected String getCacheKey(Request request) throws IOException {
        String cacheKey = request.url().toString();
        if (request.method().equalsIgnoreCase("POST")
                || request.method().equalsIgnoreCase("PATCH")) {
            String requestBody = getRequestBody(request);
            cacheKey += requestBody;
        }
        return Utils.md5(cacheKey);
    }

    public String getRequestBody(Request request) throws IOException {
        Buffer buffer = new Buffer();
        RequestBody body = request.body();
        if (body != null) body.writeTo(buffer);
        String requestBody = buffer.readUtf8();
        buffer.close();
        return requestBody;
    }

    public String getResponseBody(Response response) {
        try {
            ResponseBody body = response.body();
            BufferedSource source = body.source();
            source.request(Long.MAX_VALUE);
            Buffer buffer = source.buffer().clone();
            return buffer.readUtf8();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param request okhttp request
     * @param cache   the cache from {@link CacheInterceptor#getCache} and checked useful
     * @return response for okhttp
     */
    protected abstract Response createResponseFromCache(Request request, T cache);

    /**
     * @param cache the cache from {@link CacheInterceptor#getCache}
     * @return true if the cache is useful or false
     */
    protected abstract boolean checkUseful(T cache);

    /**
     * use default cache time when the cache header set with wrong content.
     * you can override this method return your custom default cache time.
     *
     * @return time in minutes
     */
    public int defaultCacheTime() {
        return 60;
    }

    /**
     * get cache by key
     *
     * @param key key of cache
     * @return cache or null
     */
    public abstract T getCache(String key);

    /**
     * convert response to your custom cache entity
     *
     * @param response okhttp response
     * @return cache entity
     */
    protected abstract T convert(Response response);

    /**
     * save the cache entity
     *
     * @param key         MD5 of url (if request method is post or patch ,key is url + requestBody)
     * @param cacheEntity custom entity
     * @param cacheTime   cache time (minutes)
     */
    protected abstract void saveAsCache(String key, T cacheEntity, int cacheTime);

    /**
     * remove cache by key
     *
     * @param key key of cache
     */
    public abstract void removeCache(T key);

    /**
     * clear all cache of http
     */
    public abstract void clearCache();

    /**
     * get the default impl of cacheInterceptor
     *
     * @param context ctx
     * @return {@link DefaultCacheInterceptor}
     */
    public static CacheInterceptor getDefault(Context context) {
        CacheInterceptor interceptor = interceptors.get("default");
        if (interceptor == null) {
            interceptor = new DefaultCacheInterceptor(context);
            interceptors.put("default", interceptor);
        }
        return interceptor;
    }

    public static class DefaultCacheInterceptor extends CacheInterceptor<String> {

        private ACache diskCache;

        public DefaultCacheInterceptor(Context context) {
            diskCache = ACache.get(context, "http_cache");
        }

        @Override
        protected Response createResponseFromCache(Request request, String cache) {
            return new Response.Builder()
                    .request(request)
                    .header("Hint", "response from cache")
                    .body(ResponseBody.create(MediaType.parse("application/json"), cache))
                    .protocol(Protocol.HTTP_1_1)
                    .message("")
                    .code(200)
                    .build();
        }

        @Override
        protected boolean checkUseful(String cache) {
            return !TextUtils.isEmpty(cache);
        }

        @Override
        public String getCache(String key) {
            return diskCache.getAsString(key);
        }

        @Override
        protected String convert(Response response) {
            return getResponseBody(response);
        }

        @Override
        protected void saveAsCache(String key, String cacheEntity, int cacheTime) {
            diskCache.put(key, cacheEntity, cacheTime);
        }

        @Override
        public void removeCache(String key) {
            diskCache.remove(key);
        }

        @Override
        public void clearCache() {
            diskCache.clear();
        }
    }
}
