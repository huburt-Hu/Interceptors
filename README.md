# Interceptors

一些与业务相关的okhttp拦截器


## 导入

项目的build.gradle添加：

```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
module的build.gradle添加：
```
dependencies {
       implementation 'com.github.huburt-Hu:Interceptors:v0.1.0'
}
```

### com.huburt.interceptors.CacheInterceptor

#### 介绍

okhttp自带的CacheInterceptor只能缓存GET请求，并且需要服务端的支持（或者自定义拦截器模拟服务端支持）。
而有时候出于业务或者其他的因素，我们需要在app端控制缓存，并且可能还需要缓存post请求。
因此本人写了一个CacheInterceptor用于实现以上需求。

#### 使用

com.huburt.interceptors.CacheInterceptor是一个抽象类，使用时可以直接使用默认实现。
首先我们向okhttp的client添加我们的CacheInterceptor的默认实现：

```
client = new OkHttpClient.Builder()
        .addInterceptor(CacheInterceptor.getDefault(context))
        .build();
```
其次在需要缓存的请求上添加header：
```
Request request = new Request.Builder()
      .url("http://www.baidu.com")
      .addHeader(CacheInterceptor.HEADER_NAME, String.valueOf(60))
      .build();
```
```
@Headers({CacheInterceptor.HEADER_NAME + ":60"})
@GET("users/{user}/repos")
Call<ResponseBody> listRepos(@Path("user") String user);
```
header的key为CacheInterceptor.HEADER_NAME，value为缓存时间，单位是毫秒


#### 缓存逻辑

```
@Override
public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    //读取指定header
    String header = request.header(HEADER_NAME);
    //不为空，表示需要缓存
    if (!TextUtils.isEmpty(header)) {
        //获取缓存的key，get等请求为url，post等请求为url+requestBody 取md5
        String key = getCacheKey(request);
        //获取缓存
        T cache = getCache(key);
        //判断缓存时候可用
        if (checkUseful(cache)) {//缓存可用，从缓存中创建Response对象
            return createResponseFromCache(request, cache);
        } else {//缓存不可用，走网络
            Response response = chain.proceed(request);
            if (response.isSuccessful()) {//如果接口请求成功，则缓存结果
                int cacheTime = parseCacheTime(header);
                saveAsCache(key, convert(response), cacheTime);
            }
            return response;
        }
    }
    //没有header，表示不需要缓存，不做处理
    return chain.proceed(request);
}
```

#### 扩展

默认实现是将ResponseBody转换成String，缓存到cache磁盘。
如果不喜欢默认实现的缓存，也可以自己继承CacheInterceptor实现自己的缓存逻辑。
需要实现以下几个方法，其中T为泛型，表示缓存实体的类型：
```
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
```