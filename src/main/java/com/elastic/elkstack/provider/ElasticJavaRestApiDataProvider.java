package com.elastic.elkstack.provider;

import com.elastic.elkstack.domain.Event;
import com.elastic.elkstack.domain.EventType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Service
public class ElasticJavaRestApiDataProvider implements EsService {


    @Override
    public void createNewIndex(String indexName) throws IOException {

    }

    @Override
    public void applyNewMapping(String property, String type) throws IOException {

    }

    @Override
    public void storeEvent(Event event) throws IOException {

    }

    @Override
    public void updateEvent(Event event) throws IOException {

    }

    @Override
    public long getCount(String searchString) throws IOException {
        return 0;
    }

    @Override
    public List<Event> searchAll() throws IOException {
        return null;
    }

    @Override
    public List<Event> searchBy(EventType type) throws IOException {
        return null;
    }

    @Override
    public List<Event> searchBy(String title) throws IOException {
        return null;
    }

    @Override
    public List<Event> searchBy(LocalDate afterDate, String name) throws IOException {
        return null;
    }

    @Override
    public List<Event> search(String searchString) throws Exception {
        return null;
    }
}
