import akka.http.javadsl.marshallers.jackson.Jackson
import akka.http.javadsl.model.HttpHeader
import akka.http.javadsl.server.PathMatchers.segment
import akka.http.javadsl.model.StatusCodes
import akka.http.javadsl.server.AllDirectives
import akka.http.javadsl.server.Route
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

val PATH_PARAM_REGEX = Regex("\\{\\w+}")

class RouteBuilder(private val mapper: ObjectMapper): AllDirectives() {
    fun build(mocks: List<Mock>): Route {
        val routeList = mocks.map {
            println("Registering mock route: ${it.method} ${it.path}")
            when (it.method) {
                "GET" -> get {
                    route(it)
                }
                "POST" -> post {
                    route(it)
                }

                "PUT" -> put {
                    route(it)
                }

                "DELETE" -> delete {
                    route(it)
                }

                "PATCH" -> patch {
                    route(it)
                }

                else -> get {
                    route(it)
                }
            }
        }

        return routeList.reduce { acc, route -> concat(acc, route) }
    }

    private fun route(mock: Mock): Route {
        val headers = mock.headers.map {
            HttpHeader.parse(it.name, it.value)
        }
        // Routes are sensitive to / at the begging of the path
        val path = if (mock.path.startsWith("/")) mock.path.substring(1 until mock.path.length) else mock.path
        return if (path.contains(PATH_PARAM_REGEX)) pathParamRoute(path, headers, mock.response, mock.statusCode) else simpleRoute(path, headers, mock.response, mock.statusCode)
    }

    private fun pathParamRoute(path: String, headers: Iterable<HttpHeader>, response: JsonNode, statusCode: Int) : Route {
        val paramName = PATH_PARAM_REGEX.find(path)!!.value.substringBefore("}").substring(1)
        val pathPrefix = path.substring(0, path.indexOf("{")).substringBefore("/")

        return pathPrefix(pathPrefix) {
            return@pathPrefix path(segment()) { param ->
                val modifiedResponse = this.replaceResponseValue(response, paramName, param)
                return@path complete(StatusCodes.get(statusCode), headers, modifiedResponse, Jackson.marshaller())
            }
        }
    }


    private fun simpleRoute(path: String, headers: Iterable<HttpHeader>, response: JsonNode, statusCode: Int): Route {
        return path(path) {
            complete(StatusCodes.get(statusCode), headers, response, Jackson.marshaller())
        }
    }

    /// DEBT: Avoid recursion
    private fun replaceResponseValue(response: JsonNode, paramName: String, paramValue: String): JsonNode {
        val variableName = "$$paramName"
        println(response.nodeType)
        return if (response.isObject) {
            val modifiedNode = mapper.createObjectNode()
            val objectNode = response as ObjectNode
            objectNode.fields().forEach { field ->
                println("Field: ${field.key} = ${field.value}. Type = ${field.value.nodeType}")
                val newKey = field.key.replace(variableName, paramValue)
                if (field.value.isTextual) {
                    modifiedNode.put(newKey, field.value.asText().replace(variableName, paramValue))
                } else {
                    if (field.value.isArray && !(field.value as ArrayNode).isEmpty && (field.value as ArrayNode)[0].isObject
                        || field.value.isObject ) {
                        modifiedNode.set<ObjectNode>(newKey, replaceResponseValue(field.value, paramName, paramValue))
                    } else {
                        // probably array of text
                        val newArray = mapper.createArrayNode()
                        (field.value as ArrayNode).iterator().forEach {
                            newArray.add(it.asText().replace(variableName, paramValue))
                        }

                        modifiedNode.set<ArrayNode>(newKey, newArray)
                    }
                }

                println("Modified node: $modifiedNode")
            }

            modifiedNode
        } else {
            val modifiedArray = mapper.createArrayNode()
            val arrayNode = response as ArrayNode
            arrayNode.iterator().forEach {
                val newObj = replaceResponseValue(it, paramName, paramValue)
                modifiedArray.add(newObj)
            }

            modifiedArray
        }
    }
}