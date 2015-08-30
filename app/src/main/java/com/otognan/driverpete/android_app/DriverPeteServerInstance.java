package com.otognan.driverpete.android_app;


import com.squareup.okhttp.OkHttpClient;

import java.util.concurrent.TimeUnit;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.OkClient;

public class DriverPeteServerInstance {

    public static final String serverUrl = "https://192.168.1.2:8443";
    //private static final String serverUrl = "https://testbeanstalkenv-taz59dxmiu.elasticbeanstalk.com";


    public static DriverPeteServer getInstance(final String token, int timeoutSeconds) {
        RequestInterceptor requestInterceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                request.addHeader("X-AUTH-TOKEN", token);
            }
        };

        OkHttpClient httpClient = UnsafeHttpsClient.getUnsafeOkHttpClient();
        if (timeoutSeconds != 0) {
            httpClient.setReadTimeout(timeoutSeconds, TimeUnit.SECONDS);
            httpClient.setConnectTimeout(timeoutSeconds, TimeUnit.SECONDS);
        }
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(serverUrl)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setClient(new OkClient(httpClient))
                .setRequestInterceptor(requestInterceptor)
                .build();


        return restAdapter.create(DriverPeteServer.class);
    }
}
