package com.zing.compass.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String userId;
    private String name;
    private int reviewCount;
    private int fans; // 粉丝数
    private int elite; // 精英会员等级
    private LocalDateTime yelpingSince; // 加入时间
}
//{"user_id":"qVc8ODYU5SZjKXVBgXdI7w","name":"Walker","review_count":585,"yelping_since":"2007-01-25 16:47:26","useful":7217,"funny":1259,"cool":5994,"elite":"2007","friends":"NSCy54eWehBJyZdG2iE84w, pe42u7DcCH2QmI81NX-8qA, _xgjhgNZ5H6qQolujfDeRQ, mPmbOkH3NxXbdoJ-lejjzw, 34pnOU_2J1U98ZhZKS6ykA, VX6NGOZ3-SzyUVJTnySXCw, 89QWkq_bB3cT9GRku5x-xQ, bOJ54n_SlEg5w0QbZwuRFQ, 4DFmd5LCXoLgHtYBytJJPA, SXfvHWScLghzoTSBqb_e6w, KBEsvydMCNHYFY6CIiJtQg, LYGGtE2B0zSna9v6p3rcvg, mEjPkT-wxU8kBm2zj2Cmzw","fans":267,"average_stars":3.91,"compliment_hot":250,"compliment_more":65,"compliment_profile":55,"compliment_cute":56,"compliment_list":18,"compliment_note":232,"compliment_plain":844,"compliment_cool":467,"compliment_funny":467,"compliment_writer":239,"compliment_photos":180}
