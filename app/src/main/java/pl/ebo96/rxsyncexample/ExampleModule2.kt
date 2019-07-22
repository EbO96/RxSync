package pl.ebo96.rxsyncexample

import okhttp3.ResponseBody
import pl.ebo96.rxsync.sync.builder.ModuleBuilder
import pl.ebo96.rxsync.sync.method.RxMethod
import pl.ebo96.rxsync.sync.module.RxModule

class ExampleModule2 : ModuleBuilder<Any>() {

    override fun build(builder: RxModule.Builder<Any>): RxModule<out Any> {
        RestApi.photos.forEach { url ->
            builder.register(RxMethod.create<ResponseBody>(false).registerOperation(RestApi.dogsRestService.fromUrl(url)))
        }

        return builder
                .build()
    }

    override fun isDeferred(): Boolean {
        return true
    }
}