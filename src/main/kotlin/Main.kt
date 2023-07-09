import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.http.javadsl.Http
import akka.http.javadsl.ServerBinding
import akka.http.javadsl.marshallers.jackson.Jackson
import akka.http.javadsl.model.StatusCodes
import akka.http.javadsl.server.AllDirectives
import akka.http.javadsl.server.PathMatchers.longSegment
import akka.http.javadsl.server.Route
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

@Throws(Exception::class)
fun main(args: Array<String>) {
    val port = System.getProperty("http.port", "8080").toInt()
    val folder = System.getProperty("mock.source-folder", "./examples")
    val mapper = ObjectMapper().registerModule(
        KotlinModule.Builder()
            .withReflectionCacheSize(512)
            .configure(KotlinFeature.NullToEmptyCollection, true)
            .configure(KotlinFeature.NullToEmptyMap, true)
            .configure(KotlinFeature.NullIsSameAsDefault, true)
            .configure(KotlinFeature.StrictNullChecks, false)
            .build()
    ).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val mocksExplorer = MocksExplorer(mapper)
    val mocks = mocksExplorer.findMocks(folder)

    val route = RouteBuilder(mapper).build(mocks)

    // boot up server using the route as defined below
    val system: ActorSystem<Void> = ActorSystem.create(Behaviors.empty(), "routes")
    val http: Http = Http.get(system)

    //In order to access all directives we need an instance where the routes are define.
    val binding: CompletionStage<ServerBinding> = http.newServerAt("localhost", port)
        .bind(route)
//        .bind(Routes().createRoute())
    println("Server online at http://localhost:$port/")
}

data class Item(val name: String, val id: Long)
data class Order(val items: List<Item>)
// (fake) async database query api
private fun fetchItem(itemId: Long): CompletionStage<Item?>{
    return CompletableFuture.completedFuture(Item("foo", itemId))
}

// (fake) async database query api
private fun saveOrder(order: Order): CompletionStage<Done> {
    return CompletableFuture.completedFuture(Done.getInstance())
}
class Routes: AllDirectives() {

    fun createRoute(): Route {
        return concat(
            get {
                pathPrefix("item") {
                    path(longSegment()) { id ->
                        return@path onSuccess(fetchItem(id)) { maybeItem ->
                            return@onSuccess maybeItem?.let { item ->
                                completeOK(item, Jackson.marshaller<Item>())
                            } ?: complete(StatusCodes.NOT_FOUND, "Not found")
                        }
                    }
                }
            },
            post {
                path("create-order") {
                    entity(Jackson.unmarshaller(Order::class.java)) { order ->
                        return@entity onSuccess(saveOrder(order)) { _ ->
                            complete("Order created")
                        }
                    }
                }
            }
        )
    }
}
