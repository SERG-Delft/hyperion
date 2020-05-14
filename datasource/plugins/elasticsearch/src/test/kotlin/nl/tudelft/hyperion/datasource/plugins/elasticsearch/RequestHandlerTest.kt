package nl.tudelft.hyperion.datasource.plugins.elasticsearch

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.lucene.search.TotalHits
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.SearchHits
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.lang.NullPointerException

class RequestHandlerTest {
    @Test
    fun `test successful response`() {
        val mockAction = mockk<(SearchHit) -> Unit>()
        val requestHandler = RequestHandler(action = mockAction)

        val mockResponse = mockk<SearchResponse>(relaxed = true)
        val searchHit = SearchHit(1)
        val searchHits = SearchHits(arrayOf(searchHit), TotalHits(1, TotalHits.Relation.EQUAL_TO), 1f)
        every { mockResponse.hits } returns searchHits

        requestHandler.onResponse(mockResponse)

        verify { mockAction.invoke(searchHit) }
    }

    @Test
    fun `test exception during response handling`() {
        val mockAction = mockk<(SearchHit) -> Unit>()
        val requestHandler = RequestHandler(action = mockAction)

        val mockResponse = mockk<SearchResponse>(relaxed = true)
        every { mockAction.invoke(any()) } throws NullPointerException()

        // assert that requestHandler fails silently
        assertDoesNotThrow { requestHandler.onResponse(mockResponse) }
    }

    @Test
    fun `test failed response`() {
        val mockAction = mockk<(SearchHit) -> Unit>()
        val requestHandler = RequestHandler(action = mockAction)

        // assert that requestHandler fails silently
        assertDoesNotThrow { requestHandler.onFailure(NullPointerException()) }
    }
}