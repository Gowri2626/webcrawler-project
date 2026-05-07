package com.udacity.webcrawler.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.nio.file.StandardOpenOption;

public final class CrawlResultWriter {
  private final CrawlResult result;
  private final ObjectMapper objectMapper;

public CrawlResultWriter(CrawlResult result) {
    this.result = result;

    objectMapper = new ObjectMapper();
    objectMapper.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
}

public void write(Writer writer) throws IOException {
     ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

    objectMapper.writeValue(writer, result);
}
 public void write(Path path) throws IOException {
    try (Writer writer = Files.newBufferedWriter(
            path,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
    )) {
        write(writer); 
    }
}
  
  
}
