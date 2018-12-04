package com.app.spokeo.peoplesearch.configuration;


import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface RESTService {
    @GET("{url}")
    Call<Config> getConfig(@Path(value = "url", encoded = true) String url);
}
