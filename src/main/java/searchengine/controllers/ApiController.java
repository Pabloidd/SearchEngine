package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

/**
 * Основной API контроллер для обработки запросов
 */
@RestController
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService,
                         IndexingService indexingService,
                         SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        boolean started = indexingService.startIndexing();
        return started ?
                ResponseEntity.ok(new IndexingResponse(true)) :
                ResponseEntity.badRequest()
                        .body(new IndexingResponse(false, "Индексация уже запущена"));
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        boolean stopped = indexingService.stopIndexing();
        return stopped ?
                ResponseEntity.ok(new IndexingResponse(true)) :
                ResponseEntity.badRequest()
                        .body(new IndexingResponse(false, "Индексация не запущена"));
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(@RequestParam String url) {
        boolean indexed = indexingService.indexPage(url);
        return indexed ?
                ResponseEntity.ok(new IndexingResponse(true)) :
                ResponseEntity.badRequest()
                        .body(new IndexingResponse(false, "Не удалось проиндексировать страницу"));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(
            @RequestParam String query,
            @RequestParam(required = false) String site,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(searchService.search(query, site, offset, limit));
    }
}