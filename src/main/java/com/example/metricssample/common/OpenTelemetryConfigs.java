package com.example.metricssample.common;

import com.google.cloud.opentelemetry.trace.TraceConfiguration;
import com.google.cloud.opentelemetry.trace.TraceExporter;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class OpenTelemetryConfigs {

    private final ProjectConfigs projectConfigs;
    public OpenTelemetryConfigs(ProjectConfigs projectConfigs) {
        this.projectConfigs  = projectConfigs;
    }

    @Bean
    public OpenTelemetry openTelemetry() throws IOException {
        Resource resource = Resource.builder().build();

        SpanExporter traceExporter = TraceExporter.createWithConfiguration(
                TraceConfiguration.builder()
                        .setProjectId(projectConfigs.getProjectId())
                        .build());

        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(traceExporter).build())
                .setResource(resource)
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .build();
    }
}
