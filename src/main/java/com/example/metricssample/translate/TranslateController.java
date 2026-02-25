
package com.example.metricssample.translate;

import com.example.metricssample.common.ProjectConfigs;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.tracing.ApiTracerFactory;
import com.google.api.gax.tracing.OpenTelemetryTraceManager;
import com.google.api.gax.tracing.SpanTracerFactory;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.cloud.translate.v3.TranslationServiceClient;
import com.google.cloud.translate.v3.TranslationServiceSettings;
import io.opentelemetry.api.OpenTelemetry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.threeten.bp.Duration;

import java.util.List;

@RestController
@RequestMapping(path = "/translate")
public class TranslateController {
  private final TranslationServiceClient translationServiceClient;
  private final OpenTelemetry openTelemetry;

  private final ProjectConfigs projectConfigs;

  TranslateController(OpenTelemetry openTelemetry, ProjectConfigs projectConfigs) throws Exception{
    this.openTelemetry = openTelemetry;
    this.projectConfigs = projectConfigs;
    
    TranslationServiceSettings.Builder settingsBuilder = TranslationServiceSettings.newBuilder();
    
    settingsBuilder.getStubSettingsBuilder().setTracerFactory(createOpenTelemetryTracerFactory());
    
    // Configure retry settings to trigger multiple attempts
    // We set a very short timeout and many retries to observe multiple spans in the trace
    RetrySettings retrySettings = RetrySettings.newBuilder()
            .setInitialRetryDelay(Duration.ofMillis(100))
            .setRetryDelayMultiplier(1.3)
            .setMaxRetryDelay(Duration.ofMillis(500))
            .setInitialRpcTimeout(Duration.ofMillis(1)) // Extremely short timeout to force retries
            .setRpcTimeoutMultiplier(1.0)
            .setMaxRpcTimeout(Duration.ofMillis(1))
            .setTotalTimeout(Duration.ofMillis(2000))
            .build();
    
    settingsBuilder.translateTextSettings().setRetrySettings(retrySettings);

    translationServiceClient = TranslationServiceClient.create(settingsBuilder.build());
  }

  private ApiTracerFactory createOpenTelemetryTracerFactory() {
    return new SpanTracerFactory(new OpenTelemetryTraceManager(openTelemetry));
  }

  @GetMapping(path = "/{text}", produces = "application/json")
  public String translate(@PathVariable String text) {
    LocationName locationName = LocationName.of(projectConfigs.getProjectId(), "global");
    try {
      TranslateTextResponse response = translationServiceClient.translateText(locationName, "es", List.of(text));
      String result = response.getTranslations(0).getTranslatedText();
      System.out.println("Received translation: " + result);
      return result;
    } catch (Exception e) {
      return "Translation failed (expected if forcing retries): " + e.getMessage();
    }
  }

}
