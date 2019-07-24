package pl.ebo96.rxsyncexample

import com.google.gson.annotations.SerializedName

data class DogsApiResponse(@SerializedName("message") val message: String, @SerializedName("status") val status: String) {
}