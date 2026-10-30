package com.anjo.statisticsui

import com.anjo.statisticsui.model.Channel
import com.anjo.statisticsui.model.PubSubInfo
import com.anjo.statisticsui.model.Source
import com.anjo.statisticsui.model.ingestAddress
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith

class IngestAddressTest :
    FunSpec({
        val push = Source(id = "station", dataset = "weather")
        val pubsub = push.copy(channel = Channel("pubsub"))

        test("pushSourcesShowTheirIngestUrl") {
            push.ingestAddress(PubSubInfo("kafka", "stats.ingest")) shouldBe "Push address: /api/v2/ingest/station"
        }

        test("kafkaSourcesShowTopicAndKey") {
            pubsub.ingestAddress(PubSubInfo("kafka", "stats.ingest")) shouldStartWith "Kafka topic stats.ingest, record key station"
        }

        test("jetStreamSourcesShowTheirSubject") {
            pubsub.ingestAddress(PubSubInfo("jetstream", "stats.ingest")) shouldStartWith "JetStream subject stats.ingest.station"
        }

        test("pubsubWithoutBrokerSaysSo") {
            pubsub.ingestAddress(null) shouldContain "BROKER=none"
        }
    })
