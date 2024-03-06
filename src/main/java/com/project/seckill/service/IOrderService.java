package com.project.seckill.service;

import com.project.seckill.pojo.Order;
import com.baomidou.mybatisplus.extension.service.IService;
import com.project.seckill.pojo.User;
import com.project.seckill.vo.GoodsVo;
import com.project.seckill.vo.OrderDetailVo;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author ty
 * @since 2024-03-03
 */
public interface IOrderService extends IService<Order> {
    static final int DEFAULT_MAX_RETRIES = 20;
    Order secKill(User user, GoodsVo goods);

    OrderDetailVo detail(Long orderId);

    String createPath(User user, Long goodsId);

    boolean checkPath(User user, Long goodsId, String path);

    boolean checkCaptcha(User user, Long goodsId, String captcha);
}
