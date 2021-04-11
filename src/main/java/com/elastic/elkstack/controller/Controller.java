package com.elastic.elkstack.controller;

import com.elastic.elkstack.domain.Event;
import com.elastic.elkstack.provider.EsService;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/event")
@Log4j2
public class Controller {

    EsService esService;

    @GetMapping
    public UUID getUuid() {
        UUID uuid = UUID.randomUUID();
        log.info("Generates some log {}", uuid);
        return uuid;
    }


    @GetMapping("/count")
    public Long getCount(@RequestParam("query") String query) throws IOException {
        return esService.getCount(query);
    }

    @PutMapping("/update")
    public String updateEvent(@RequestBody Event event) throws IOException {
        String id = UUID.randomUUID().toString();
        esService.updateEvent(event);
        return id;
    }

    @GetMapping("/search")
    public List<Event> search(@RequestParam("query") String query) throws Exception {
        return esService.search(query);
    }

    @PutMapping("/index")
    public void createNewIndex(@RequestParam("indexName") String indexName) throws Exception {
        esService.createNewIndex(indexName);
    }
}
