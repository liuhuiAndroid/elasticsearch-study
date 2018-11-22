package com.elasticsearch.bean;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class BookBean {

    private String id;

    private String title;

    private String author;

    private Integer word_count;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date publish_date;

}
