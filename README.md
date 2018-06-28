# Interceptors

一些与业务相关的okhttp拦截器


## 介绍

*com.huburt.interceptors.CacheInterceptor*  

okhttp自带的CacheInterceptor只能缓存GET请求，并且需要服务端的支持（或者自定义拦截器模拟服务端支持）。
而有时候出于业务或者其他的因素，我们需要在app端控制缓存，并且
