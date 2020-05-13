package nl.tudelft.hyperion.plugin.connection

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.module.kotlin.KotlinModule
import nl.tudelft.hyperion.plugin.metric.IntervalMetric
import nl.tudelft.hyperion.plugin.metric.LineMetrics
import nl.tudelft.hyperion.plugin.metric.Metric
import nl.tudelft.hyperion.plugin.metric.MetricsResult

class MetricDeserializer : JsonDeserializer<MetricsResult>() {
    private val mapper = ObjectMapper(JsonFactory())
    init {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) // ignore weird properties
        mapper.registerModule(KotlinModule())
    }

    override fun deserialize(p: JsonParser?,
                             ctxt: DeserializationContext?): MetricsResult {
        val versions: Map<String, List<LineMetrics>>

        val tempVersions: MutableMap<String, MutableList<IntervalMetric>> = mutableMapOf()
        val node: JsonNode = p!!.codec.readTree(p)
        for (intervals in node) {
            val interval = intervals["interval"].asInt()
            for (version in intervals["versions"].fields()) {

                val valuesToAdd = version.value.map {
                    v: JsonNode ->
                    IntervalMetric(interval, mapper.readValue(v.traverse(), Metric::class.java))
                }
                if (tempVersions[version.key] != null) tempVersions[version.key]!!.addAll(valuesToAdd)
                else tempVersions[version.key] = valuesToAdd.toMutableList()

            }
        }
        versions = tempVersions
                .mapValues {
                    entry -> entry.value.groupBy { metric -> metric.metric.line }
                        .map { groupedEntry -> LineMetrics(groupedEntry.value) }
                }

        return MetricsResult(versions)
    }

}