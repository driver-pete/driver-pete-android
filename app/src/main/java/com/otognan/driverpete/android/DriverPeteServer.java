package com.otognan.driverpete.android;


import retrofit.client.Response;
import retrofit.http.GET;

public interface DriverPeteServer {
//    @GET("/api/user/current")
//    User getCurrentUser();

    @GET("/api/user/current")
    Response userListResponse();
}
