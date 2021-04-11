package com.elastic.elkstack.provider;


import com.elastic.elkstack.domain.Event;
import com.elastic.elkstack.domain.EventType;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface EsService {
    void createNewIndex(String indexName) throws IOException;

    void applyNewMapping(String property, String type) throws IOException;

    void storeEvent(Event event) throws IOException;

    void updateEvent(Event event)throws IOException;

    long getCount(String searchString)throws IOException;

    List<Event> searchAll()throws IOException;

    List<Event> searchBy(EventType type)throws IOException;

    List<Event> searchBy(String title)throws IOException;

    List<Event> searchBy(LocalDate afterDate, String name)throws IOException;

    List<Event> search(String searchString) throws Exception;
}
