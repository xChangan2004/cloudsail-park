package com.changan.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileVO {

    private Long id;

    private String url;

    private String name;

    private Long size;

    private String contentType;
}