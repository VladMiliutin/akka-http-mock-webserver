package xyz.vladm.akka_mock_server

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File

class MocksExplorer(private val mapper: ObjectMapper) {

    fun findMocks(folder: String): List<Mock> {
        val file = File(folder)
        if (!file.isDirectory) {
           return emptyList()
        }

        val mockFiles = file.listFiles { _, name -> name.endsWith(".json") }

        return mockFiles?.map {
            val mock = mapper.readValue(it, Mock::class.java)
            mock
        } ?: emptyList()
    }
}
data class MockHeader(
    val name: String,
    val value: String
)

data class Mock(
    val name: String,
    val method: String,
    val path: String,
    val statusCode: Int = 200,
    val simulateStreaming: Boolean = false,
    val headers: Iterable<MockHeader> = emptyList(),
    val response: JsonNode)