package com.elasticsearch;

import com.elasticsearch.bean.BookBean;
import com.elasticsearch.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@SpringBootApplication
@RestController
@Slf4j
public class ElasticsearchStudyApplication {

    @Autowired
    TransportClient transportClient;

    public static void main(String[] args) {
        SpringApplication.run(ElasticsearchStudyApplication.class, args);
    }

    /**
     * 查询接口
     * @param id
     * @return
     */
    @GetMapping("/get/book/novel")
    public ResponseEntity get(@RequestParam(name = "id", defaultValue = "") String id) {
        if (StringUtils.isEmpty(id)) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        GetResponse response = transportClient.prepareGet("book", "novel", id).get();
        if (!response.isExists()) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity(response.getSource(), HttpStatus.OK);
    }

    /**
     * 增加接口
     * @param bookBean
     * @return
     */
    @PostMapping("/add/book/novel")
    public ResponseEntity add(BookBean bookBean) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder().startObject()
                    .field("title", bookBean.getTitle())
                    .field("author", bookBean.getAuthor())
                    .field("word_count", bookBean.getWord_count())
                    .field("publish_date", DateUtil.dateToStr(bookBean.getPublish_date()))
                    .endObject();
            IndexResponse response = transportClient.prepareIndex("book", "novel")
                    .setSource(builder).get();
            return new ResponseEntity(response.getId(), HttpStatus.OK);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 删除接口
     * @param id
     * @return
     */
    @DeleteMapping("/delete/book/novel")
    public ResponseEntity delete(@RequestParam(name = "id", defaultValue = "") String id) {
        if (StringUtils.isEmpty(id)) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        DeleteResponse response = transportClient.prepareDelete("book", "novel", id).get();
        return new ResponseEntity(response.getResult().toString(), HttpStatus.OK);
    }

    /**
     * 更新接口
     * @param bookBean
     * @return
     */
    @PutMapping("/update/book/novel")
    public ResponseEntity update(BookBean bookBean) {
        UpdateRequest request = new UpdateRequest("book", "novel", bookBean.getId());
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder().startObject();
            if (bookBean.getAuthor() != null) {
                builder.field("author", bookBean.getAuthor());
            }
            if (bookBean.getTitle() != null) {
                builder.field("title", bookBean.getTitle());
            }
            builder.endObject();
            request.doc(builder);
            UpdateResponse response = transportClient.update(request).get();
            return new ResponseEntity(response.getResult().toString(), HttpStatus.OK);
        } catch (IOException | InterruptedException | ExecutionException e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * 复合查询
     * @param author
     * @param title
     * @param gtWordCount
     * @param ltWordCount
     * @return
     */
    @PostMapping("query/book/novel")
    public ResponseEntity query(
            @RequestParam(value = "author", required = false) String author,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "gt_word_count", defaultValue = "0") int gtWordCount,
            @RequestParam(value = "lt_word_count", required = false) Integer ltWordCount) {
        BoolQueryBuilder boolBuilder = QueryBuilders.boolQuery();
        if (author != null) {
            boolBuilder.must(QueryBuilders.matchQuery("author", author));
        }
        if (title != null) {
            boolBuilder.must(QueryBuilders.matchQuery("title", title));
        }
        RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("word_count").from(gtWordCount);
        if (ltWordCount != null && ltWordCount > 0) {
            rangeQuery.to(ltWordCount);
        }
        boolBuilder.filter(rangeQuery);
        SearchRequestBuilder builder = transportClient.prepareSearch("book")
                .setTypes("novel")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(boolBuilder)
                .setFrom(0)
                .setSize(10);
        log.info(String.valueOf(builder));
        SearchResponse response = builder.get();
        List<Map<String, Object>> result = new ArrayList<>();
        response.getHits().forEach(s -> result.add(s.getSourceAsMap()));
        return new ResponseEntity(result, HttpStatus.OK);
    }

}
