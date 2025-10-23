package com.example.starter

import com.example.starter.OpenTelemetryConfig.configure
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.tracing.opentelemetry.OpenTelemetryOptions

class MainVerticle : AbstractVerticle() {

  override fun start(startPromise: Promise<Void>) {
    val resource = Resource.getDefault().toBuilder()
      .put(AttributeKey.stringKey("service.name"), "service-b")
      .build()

    val otel = configure(resource)
    val vertx = Vertx.vertx(VertxOptions().setTracingOptions(OpenTelemetryOptions(otel)))

    vertx.createHttpServer()
      .requestHandler { req ->
        req.response()
          .putHeader("content-type", "text/plain")
          .end("Hello from trace")
      }
      .listen(8080)
      .onSuccess {
        println("HTTP server started on port 8080")
        startPromise.complete()
      }
      .onFailure(startPromise::fail)
  }
}

internal object OpenTelemetryConfig {

  fun configure(resource: Resource): OpenTelemetry {
    val exporter = OtlpGrpcSpanExporter.builder()
      .setEndpoint("http://simplest-agent:4317/v1/traces")
      .build()

    val tracerProvider = SdkTracerProvider.builder()
      .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
      .setResource(resource)
      .build()

    return OpenTelemetrySdk.builder()
      .setTracerProvider(tracerProvider)
      .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
      .buildAndRegisterGlobal()
  }
}
