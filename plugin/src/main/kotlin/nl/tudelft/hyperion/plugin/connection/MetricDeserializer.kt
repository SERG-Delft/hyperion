package nl.tudelft.hyperion.plugin.connection

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.module.kotlin.KotlinModule
import nl.tudelft.hyperion.plugin.metric.LineMetrics
import nl.tudelft.hyperion.plugin.metric.Metric
import nl.tudelft.hyperion.plugin.metric.MetricsResult

class MetricDeserializer : JsonDeserializer<List<MetricsResult<LineMetrics.IntervalMetric>>>() {
    private val mapper = ObjectMapper(JsonFactory())
    init {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) // ignore weird properties
        mapper.registerModule(KotlinModule())
    }

    override fun deserialize(p: JsonParser?,
                             ctxt: DeserializationContext?): List<MetricsResult<LineMetrics.IntervalMetric>> {
        val result: MutableList<MetricsResult<LineMetrics.IntervalMetric>> = mutableListOf()

        val node: JsonNode = p!!.codec.readTree(p)
        for (intervals in node) {
            val interval = intervals["interval"].asInt()
            val versions: MutableMap<String, List<LineMetrics.IntervalMetric>> = mutableMapOf()
            for (version in intervals["versions"].fields()) {

                versions[version.key] =
                        version.value.map {
                            v: JsonNode ->
                            LineMetrics.IntervalMetric(interval, mapper.readValue(v.traverse(), Metric::class.java))
                        }
            }
            result.add(MetricsResult(
                    interval,
                    versions
            ))
        }

        return result
    }

}