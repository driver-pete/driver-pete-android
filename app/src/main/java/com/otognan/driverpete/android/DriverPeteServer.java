package com.otognan.driverpete.android;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Query;
import retrofit.mime.TypedInput;


public interface DriverPeteServer {
    @GET("/api/user/current")
    void currentUser(Callback<User> callback);

    @POST("/api/trajectory/compressed")
    void uploadCompressedTrajectory(@Query("label") String label, @Body TypedInput body, Callback<Response> callback);
}
