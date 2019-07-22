package pl.ebo96.rxsyncexample

import io.reactivex.Flowable
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Url

interface DogsRestService {

    @GET("breeds/image/random")
    fun getRandomPhoto(): Flowable<DogsApiResponse>

    @GET
    fun fromUrl(@Url url: String): Flowable<ResponseBody>
}