package com.zing.compass.service;

import com.alibaba.fastjson2.JSON;
import com.zing.compass.dto.MerchantDTO;
import com.zing.compass.dto.UserDTO;
import com.zing.compass.entity.Comment;
import com.zing.compass.entity.UserBehavior;
import com.zing.compass.utils.UserHolder;
import com.zing.compass.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentMapper commentMapper;

    //Redis
    private final StringRedisTemplate redisTemplate;

    public boolean addComment(String bizId, Integer score, String content) {
        UserDTO currentUser = UserHolder.getUser();
        if (currentUser == null) {
            throw new RuntimeException("用户未登录");
        }
        String userId = currentUser.getUserId();
        String commentId = UUID.randomUUID().toString();
        Comment comment = new Comment(commentId, userId, bizId, score, content, LocalDateTime.now());

        return commentMapper.insertComment(comment) > 0;
    }

    //获取近期评论记录
    public List<UserBehavior> getUserRecentCommentBiz(String userId, Integer limit) {
        //1.Redis
        String key = "user:comment:" + userId;

        //1.Redis
        List<String> jsonList = redisTemplate.opsForList().range(key, 0, limit - 1);

        if (!CollectionUtils.isEmpty(jsonList)) {
            List<UserBehavior> recentComment = new ArrayList<>();
            for (String json : jsonList) {
                // 手动反序列化
                UserBehavior behavior = JSON.parseObject(json, UserBehavior.class);
                recentComment.add(behavior);
            }
            return recentComment;
        }

        //2.MySQL
        List<UserBehavior> recentComment = commentMapper.selectRecentCommentBiz(userId, limit);

        //3.缓存到Redis
        if (recentComment != null && !recentComment.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(key, recentComment.stream().map(JSON::toJSONString).toList());
            redisTemplate.expire(key, 24, TimeUnit.HOURS); //设置过期时间
        }


        return recentComment;
    }

    public Map<String, Object> getCurrentMerchantCommentPage(Integer pageNo, Integer pageSize) {
        MerchantDTO currentMerchant = UserHolder.getMerchant();
        if (currentMerchant == null || currentMerchant.getBizId() == null || currentMerchant.getBizId().isBlank()) {
            throw new RuntimeException("商家未登录");
        }

        int safePageNo = (pageNo == null || pageNo < 1) ? 1 : pageNo;
        int safePageSize = (pageSize == null || pageSize < 1) ? 10 : Math.min(pageSize, 50);
        int offset = (safePageNo - 1) * safePageSize;

        String bizId = currentMerchant.getBizId();
        List<Comment> list = commentMapper.selectCommentsByBizIdPage(bizId, offset, safePageSize);
        Long total = commentMapper.countCommentsByBizId(bizId);

        Map<String, Object> pageData = new HashMap<>();
        pageData.put("list", list == null ? new ArrayList<>() : list);
        pageData.put("total", total == null ? 0L : total);
        pageData.put("pageNo", safePageNo);
        pageData.put("pageSize", safePageSize);
        return pageData;
    }
}
