package com.otognan.driverpete.android;

import java.util.List;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Query;
import retrofit.mime.TypedInput;


public interface DriverPeteServer {
    @GET("/api/user/current")
    void currentUser(Callback<User> callback);

    @POST("/api/trajectory/compressed")
    void uploadCompressedTrajectory(@Query("label") String label, @Body TypedInput body, Callback<Response> callback);

    @GET("/api/trajectory/endpoints")
    public void trajectoryEndpoints(Callback<List<TrajectoryEndpoint>> callback);

    @POST("/api/trajectory/endpoints")
    public void editEndpoint(@Body TrajectoryEndpoint endpoint, Callback<Response> callback);

    @GET("/api/trajectory/routes")
    public void routes(@Query("isAtoB") boolean isAtoB, Callback<List<Route>> callback);

    @GET("/api/trajectory/routes/binary")
    public void binaryRoute(@Query("routeId") long routeId, Callback<Response> callback);

    @DELETE("/api/trajectory/state")
    public void resetProcessorState(Callback<Response> callback);

    @DELETE("/api/trajectory/processed")
    public void deleteProcessedUserData(Callback<Response> callback);

    @DELETE("/api/trajectory/endpoints/all")
    public void deleteAllEndpoints(Callback<Response> callback);

    @DELETE("/api/trajectory/routes/all")
    public void deleteAllRoutes(Callback<Response> callback);

    @GET("/api/trajectory/reprocess/all")
    public void reprocessAllUserData(Callback<Response> callback);
}
