package xyz.vladm.akka_mock_server

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
        return if (mock.method == "GET" && mock.response.isArray)
                paginationRoute(path, headers, mock.response as ArrayNode, mock.statusCode)
            else if (path.contains(PATH_PARAM_REGEX))
                pathParamRoute(path, headers, mock.response, mock.statusCode)
            else
                simpleRoute(path, headers, mock.response, mock.statusCode)
    }

    // DEBT: Minor duplication
    private fun paginationRoute(path: String, headers: Iterable<HttpHeader>, response: ArrayNode, statusCode: Int) : Route {
        return if (path.contains(PATH_PARAM_REGEX)) {
            val paramName = PATH_PARAM_REGEX.find(path)!!.value.substringBefore("}").substring(1)
            val pathPrefix = path.substring(0, path.indexOf("{")).substringBefore("/")

            return pathPrefix(pathPrefix) {
                return@pathPrefix path(segment()) { param ->
                    val modifiedResponse = this.replaceResponseValue(response, paramName, param)
                    sortingAndPagination(modifiedResponse as ArrayNode, statusCode, headers)
                }
            }
        } else {
            return path(path) {
                sortingAndPagination(response, statusCode, headers)
            }
        }
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

    private fun sortResponse(response: ArrayNode, sortBy: String, sortDirection: String): ArrayNode {
        val sortedResponse = mapper.createArrayNode()
        val asList = ArrayList(response.toList())

        val comparator: Comparator<JsonNode> = Comparator { o1, o2 ->
            o1.get(sortBy).asText().compareTo(o2.get(sortBy).asText())
        }

        val comparatorWithOrder = if (sortDirection == "desc") {
            comparator.reversed()
        } else {
            comparator
        }
        QuickSort.sort(asList, comparatorWithOrder)

        return sortedResponse.addAll(asList)
    }

    private fun paginate(res: ArrayNode, params: Map<String, String>): ArrayNode {
        val page = params["page"]!!.toInt()
        val pageSize = params["pageSize"]!!.toInt()
        val start = (page - 1) * pageSize
        val end = start + pageSize
        val paginatedResponse = mapper.createArrayNode()

        for (i in start until end) {
            if (i >= res.size()) {
                break
            }
            paginatedResponse.add(res.get(i))
        }

        return paginatedResponse
    }

    private fun sort(res: ArrayNode, params: Map<String, String>): ArrayNode {
        val sortBy = params["sortBy"]!!
        val sortDirection = params["sortDirection"] ?: "asc"

        return sortResponse(res, sortBy, sortDirection)
    }

    private fun sortingAndPagination(res: ArrayNode, code: Int, httpHeaders: Iterable<HttpHeader>): Route {
        return parameterMap { params ->
            var modifiedResponse = mapper.createArrayNode()
            if (params.contains("sortBy")) {
                modifiedResponse.addAll(sort(res, params))
            }

            if (modifiedResponse.isEmpty) {
                modifiedResponse = res
            }

            if (params.contains("page")) {
                modifiedResponse = paginate(modifiedResponse, params)
            }

            return@parameterMap complete(
                StatusCodes.get(code),
                httpHeaders,
                modifiedResponse,
                Jackson.marshaller()
            )
        }
    }
    /// DEBT: Avoid recursion
    private fun replaceResponseValue(response: JsonNode, paramName: String, paramValue: String): JsonNode {
        val variableName = "$$paramName"
        return if (response.isObject) {
            val modifiedNode = mapper.createObjectNode()
            val objectNode = response as ObjectNode
            objectNode.fields().forEach { field ->
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