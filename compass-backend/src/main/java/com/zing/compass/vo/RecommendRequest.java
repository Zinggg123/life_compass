package com.zing.compass.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendRequest {
    @JsonProperty("user_id")
    String userId;
    @JsonProperty("page_id")
    Integer pageId;
}
