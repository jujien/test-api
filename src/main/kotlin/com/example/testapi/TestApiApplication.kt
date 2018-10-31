package com.example.testapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono

@SpringBootApplication
class TestApiApplication

fun main(args: Array<String>) {
    runApplication<TestApiApplication>(*args)
}

@Document(collection = "location")
data class Location(@Id var id: String? = null, var latitude: Double, var longitude: Double)

data class GenericResponse(val message: String = "Succeeded!", val status: String = "OK")

fun generic(block: (GenericResponse) -> Unit): GenericResponse {
    val response = GenericResponse()
    block(response)
    return response
}

interface LocationRepository: ReactiveMongoRepository<Location?, String>

@Service
class LocationService(private val locationRepository: LocationRepository) {
    fun findAll() = this.locationRepository.findAll().filter { it != null }.map { it!! }

    fun save(location: Flux<Location>) = this.locationRepository.saveAll(location)
}


@Configuration
class Router {

    @Bean
    fun routes(locationService: LocationService) = router {
        "/location".nest {
            this.GET("/all", { ServerResponse.ok().body(locationService.findAll(), Location::class.java) })
            this.POST("/list", {
                locationService
                        .save(it.bodyToFlux(Location::class.java))
                        .toMono()
                        .flatMap { ServerResponse.ok().body(
                                Mono.just(generic {  }),
                                GenericResponse::class.java
                        ) }
            })
        }
    }
}
