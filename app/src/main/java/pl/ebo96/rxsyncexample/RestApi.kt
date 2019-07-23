package pl.ebo96.rxsyncexample

import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

object RestApi {

    val results: ArrayList<String> = arrayListOf()

    val dogsRestService: DogsRestService = let {
        val retrofit = Retrofit.Builder()
                .baseUrl("https://dog.ceo/api/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        retrofit.create(DogsRestService::class.java)
    }
}