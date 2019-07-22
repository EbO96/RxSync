package pl.ebo96.rxsyncexample

import pl.ebo96.rxsync.sync.builder.ModuleBuilder
import pl.ebo96.rxsync.sync.method.RxMethod
import pl.ebo96.rxsync.sync.module.RxModule

class ExampleModule : ModuleBuilder<Any>() {

    override fun build(builder: RxModule.Builder<Any>): RxModule<Any> {
        return builder
                .asyncMethodsAttemptsDelay(500)
                .asyncMethodsRetryAttempts(2)
                .register(RxMethod.create<DogsApiResponse>(false).registerOperation(RestApi.dogsRestService.getRandomPhoto()))
                .register(RxMethod.create<DogsApiResponse>(false).registerOperation(RestApi.dogsRestService.getRandomPhoto()))
                .register(RxMethod.create<DogsApiResponse>(false).registerOperation(RestApi.dogsRestService.getRandomPhoto()))
                .register(RxMethod.create<DogsApiResponse>(false).registerOperation(RestApi.dogsRestService.getRandomPhoto()))
                .register(RxMethod.create<DogsApiResponse>(false).registerOperation(RestApi.dogsRestService.getRandomPhoto()))
                .register(RxMethod.create<DogsApiResponse>(false).registerOperation(RestApi.dogsRestService.getRandomPhoto()))
                .register(RxMethod.create<DogsApiResponse>(false).registerOperation(RestApi.dogsRestService.getRandomPhoto()))
                .register(RxMethod.create<DogsApiResponse>(false).registerOperation(RestApi.dogsRestService.getRandomPhoto()))
                .register(RxMethod.create<DogsApiResponse>(false).registerOperation(RestApi.dogsRestService.getRandomPhoto()))
                .register(RxMethod.create<DogsApiResponse>(false).registerOperation(RestApi.dogsRestService.getRandomPhoto()))
                .register(RxMethod.create<DogsApiResponse>(false).registerOperation(RestApi.dogsRestService.getRandomPhoto()))
                .build()
    }
}