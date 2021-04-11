package com.elastic.elkstack.provider;

import com.elastic.elkstack.domain.Event;
import com.elastic.elkstack.domain.EventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Service
@RequiredArgsConstructor
public class ElasticJavaApiDataProvider implements EsService {

    @Getter
    @Setter
    @AllArgsConstructor
    private static class IndexMappingRequest {
        private Map<String, TypeClass> properties;

        @Getter
        @Setter
        @AllArgsConstructor
        private static class TypeClass {
            private String type;
        }
    }


    private final ObjectMapper mapper;
    private final RestHighLevelClient restHighLevelClient;

    private static final String INDEX_NAME = "event_index";

    @Override
    public void createNewIndex(String indexName) throws IOException {
        IndexRequest indexRequest = new IndexRequest(indexName);
        restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
    }

    @Override
    public void applyNewMapping(String property, String type) throws IOException {
        PutMappingRequest request = new PutMappingRequest(INDEX_NAME);
        IndexMappingRequest.TypeClass newType = new IndexMappingRequest.TypeClass(type);
        IndexMappingRequest indexMappingRequest = new IndexMappingRequest(Map.of(property, newType));
        request.source(serialize(indexMappingRequest), XContentType.JSON);
        restHighLevelClient.indices().putMapping(request, RequestOptions.DEFAULT);
    }

    @Override
    public void storeEvent(Event event) throws IOException {
        IndexRequest indexRequest = new IndexRequest(INDEX_NAME);
        indexRequest.source(serialize(event), XContentType.JSON);
        restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
    }

    @Override
    public void updateEvent(Event event) throws IOException {
        IndexRequest indexRequest = new IndexRequest(INDEX_NAME);
        indexRequest.id(event.getId().toString());
        indexRequest.source(mapper.writeValueAsString(event), XContentType.JSON);
        restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
    }

    // assess the number of found docs by this query
    public long getCount(String searchString) throws IOException {
        CountRequest countRequest = new CountRequest();
        countRequest.query(QueryBuilders
                .multiMatchQuery(searchString, "title", "text")
                .fuzziness("AUTO"));
        CountResponse countResponse = restHighLevelClient.count(countRequest, RequestOptions.DEFAULT);
        return countResponse.getCount();
    }

    @Override
    public List<Event> searchAll() throws IOException {
        SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
        SearchResponse search = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        return mapResponse(search);
    }

    @Override
    public List<Event> searchBy(EventType type)  throws IOException {
        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("eventType", type);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(matchQueryBuilder);
        SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
        searchRequest.source(searchSourceBuilder);
        SearchResponse search = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        return mapResponse(search);
    }

    @Override
    public List<Event> searchBy(String title)  throws IOException {
        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("title", title);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(matchQueryBuilder);
        SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
        searchRequest.source(searchSourceBuilder);
        SearchResponse search = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        return mapResponse(search);
    }

    @Override
    public List<Event> searchBy(LocalDate afterDate, String name) throws IOException  {
        String queryString = format("datetime > %s AND title:*%s*", serialize(afterDate), name);
        QueryStringQueryBuilder queryStringQueryBuilder = QueryBuilders.queryStringQuery(queryString);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryStringQueryBuilder);
        SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
        searchRequest.source(searchSourceBuilder);
        SearchResponse search = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        return mapResponse(search);
    }

    private String serialize(Object o)  throws IOException {
        return mapper.writeValueAsString(o);
    }

    private <T> T deserialize(Class<T> clazz, Map<String, Object> response) {
        return mapper.convertValue(response, clazz);
    }

    private List<Event> mapResponse(SearchResponse search) {
        return Arrays.stream(search.getHits().getHits())
                .map(SearchHit::getSourceAsMap)
                .map(o -> deserialize(Event.class, o))
                .collect(Collectors.toList());
    }

    public List<Event> search(String searchString) throws Exception {
        SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //  searchSourceBuilder.query(QueryBuilders.matchQuery("text", searchString));
        //search by full phrase
        //  searchSourceBuilder.query(QueryBuilders.matchPhraseQuery("text", searchString));
        //search by both properties
        searchSourceBuilder.query(QueryBuilders
                .multiMatchQuery(searchString, "title", "place")
                .fuzziness("AUTO"));  //help in case of fuzzy query

        //multy query search
       /* searchSourceBuilder.query(QueryBuilders.boolQuery()
                .must(QueryBuilders.multiMatchQuery(searchString, "title", "text"))
                .mustNot(QueryBuilders.multiMatchQuery("red", "title", "text"))); //exclude hits with word "red"*/

        // Highlight matches in results
        HighlightBuilder highlightBuilder = new HighlightBuilder()
                .field(new HighlightBuilder.Field("title"))     //property to highlight found words in
                .field(new HighlightBuilder.Field("place"));     //property to highlight found words in
        searchSourceBuilder.highlighter(highlightBuilder);

        //pagination
        searchSourceBuilder
                //        .from(1)           //skip 1 hit, start the search from 1 doc (Default = 0)
                .size(10);         //number of search hits to return (page)

        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        List<Event> events = new ArrayList<>();
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            String title = (String) sourceAsMap.get("title");
            String place = (String) sourceAsMap.get("place");

            // Config highlight of matches fragments in results
            HighlightField highlightFieldTitle = hit.getHighlightFields().get("title");
            if (highlightFieldTitle != null && highlightFieldTitle.fragments().length > 0) {  //check all fragments where found words
                title = highlightFieldTitle.fragments()[0].toString();                         //highlight only first fragment
            }
            HighlightField highlightFieldPlace = hit.getHighlightFields().get("place");
            if (highlightFieldPlace != null && highlightFieldPlace.fragments().length > 0) {
                place = highlightFieldPlace.fragments()[0].toString();
            }

            Event event = new Event();
            event.setTitle(title);
            event.setPlace(place);
            events.add(event);
        }

        return events;
    }

}
