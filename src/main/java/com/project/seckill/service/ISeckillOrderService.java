package com.project.seckill.service;

import com.project.seckill.pojo.SeckillOrder;
import com.baomidou.mybatisplus.extension.service.IService;
import com.project.seckill.pojo.User;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author ty
 * @since 2024-03-03
 */
public interface ISeckillOrderService extends IService<SeckillOrder> {

    Long getResult(User user, Long goodsId);
}
