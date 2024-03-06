package com.project.seckill.vo;

import com.project.seckill.pojo.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetailVo {
    private User user;

    private GoodsVo goodsVO;

    private int secKillStatus;

    private int remainSeconds;
}