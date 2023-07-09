import akka.http.javadsl.marshallers.jackson.Jackson
import akka.http.javadsl.model.HttpHeader
import akka.http.javadsl.model.ResponseEntity
import akka.http.javadsl.model.StatusCodes
import akka.http.javadsl.server.AllDirectives
import akka.http.javadsl.server.Route
import com.fasterxml.jackson.databind.ObjectMapper

class RouteBuilder(private val mapper: ObjectMapper): AllDirectives() {
    fun build(mocks: List<Mock>): Route {
        val routeList = mocks.map {
            println("Registering mock route: ${it.method} ${it.path}")
            when (it.method) {
                "GET" -> get {
                    simpleRoute(it)
                }
                "POST" -> post {
                    simpleRoute(it)
                }

                "PUT" -> put {
                    simpleRoute(it)
                }

                "DELETE" -> delete {
                    simpleRoute(it)
                }

                "PATCH" -> patch {
                    simpleRoute(it)
                }

                else -> get {
                    simpleRoute(it)
                }
            }
        }

        return routeList.reduce { acc, route -> concat(acc, route) }
    }

    private fun simpleRoute(mock: Mock): Route {
        val headers = mock.headers.map {
            HttpHeader.parse(it.name, it.value)
        }
        // Routes are sensitive to / at the begging of the path
        val path = if (mock.path.startsWith("/")) mock.path.substring(1 until mock.path.length) else mock.path
        return path(path) {
            complete(StatusCodes.get(mock.statusCode), headers, mock.response, Jackson.marshaller())
        }
    }
}