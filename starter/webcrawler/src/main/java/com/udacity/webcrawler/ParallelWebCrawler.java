package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;
import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

final class ParallelWebCrawler implements WebCrawler {
  private final Clock clock;
  private final Duration timeout;
  private final int popularWordCount;
  private final ForkJoinPool pool;
private final int maxDepth;
private final PageParserFactory parserFactory;
private final List<Pattern> ignoredWords;
private final List<Pattern> ignoredUrls;
@Inject
ParallelWebCrawler(
    Clock clock,
    PageParserFactory parserFactory,
    @Timeout Duration timeout,
    @PopularWordCount int popularWordCount,
    @MaxDepth int maxDepth,
    @IgnoredWords List<Pattern> ignoredWords,
    @IgnoredUrls List<Pattern> ignoredUrls,
    @TargetParallelism int threadCount)  {
    this.clock = clock;
    this.timeout = timeout;
    this.popularWordCount = popularWordCount;
    this.parserFactory = parserFactory;
this.maxDepth = maxDepth;
this.ignoredWords = ignoredWords;
this.ignoredUrls = ignoredUrls;
    this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
  }

 @Override
public CrawlResult crawl(List<String> startingUrls) {

    Instant deadline = clock.instant().plus(timeout);

    Set<String> visitedUrls = ConcurrentHashMap.newKeySet();

    ConcurrentMap<String, Integer> counts =
            new ConcurrentHashMap<>();

    List<CrawlTask> tasks = new ArrayList<>();

    for (String url : startingUrls) {

        tasks.add(
                new CrawlTask(
                        url,
                        maxDepth,
                        deadline,
                        counts,
                        visitedUrls
                )
        );
    }

    for (CrawlTask task : tasks) {
        pool.invoke(task);
    }
if (counts.isEmpty()) {
    return new CrawlResult.Builder()
            .setWordCounts(counts)
            .setUrlsVisited(visitedUrls.size())
            .build();
}
    return new CrawlResult.Builder()
        .setWordCounts(
                WordCounts.sort(counts, popularWordCount)
        )
        .setUrlsVisited(visitedUrls.size())
        .build();
}

  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }
  private class CrawlTask extends RecursiveAction {

    private final String url;
    private final int depth;
    private final Instant deadline;
    private final ConcurrentMap<String, Integer> counts;
    private final Set<String> visitedUrls;

    public CrawlTask(
            String url,
            int depth,
            Instant deadline,
            ConcurrentMap<String, Integer> counts,
            Set<String> visitedUrls) {

        this.url = url;
        this.depth = depth;
        this.deadline = deadline;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
    }

    @Override
    protected void compute() {
if (depth == 0) {
    return;
}

if (clock.instant().isAfter(deadline)) {
    return;
}
for (Pattern pattern : ignoredUrls) {
    if (pattern.matcher(url).matches()) {
        return;
    }
}
if (!visitedUrls.add(url)) {
    return;
}
PageParser.Result result = parserFactory.get(url).parse();
for (Map.Entry<String, Integer> entry : result.getWordCounts().entrySet()) {
    String word = entry.getKey();
     if (word == null || word.isBlank()) {
        continue;
    }
    boolean ignored = ignoredWords.stream()
            .anyMatch(p -> p.matcher(word).matches());
    if (ignored) {
        continue;
    }
    counts.merge(word, entry.getValue(), Integer::sum);
}
List<CrawlTask> subtasks = new ArrayList<>();
for (String link : result.getLinks()) {

    subtasks.add(
            new CrawlTask(
                    link,
                    depth - 1,
                    deadline,
                    counts,
                    visitedUrls
            )
    );
}
invokeAll(subtasks);

    }
}
}
