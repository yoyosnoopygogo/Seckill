package com.project.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.project.seckill.exception.GlobalException;
import com.project.seckill.mapper.GoodsMapper;
import com.project.seckill.pojo.Order;
import com.project.seckill.mapper.OrderMapper;
import com.project.seckill.pojo.SeckillGoods;
import com.project.seckill.pojo.SeckillOrder;
import com.project.seckill.pojo.User;
import com.project.seckill.service.IGoodsService;
import com.project.seckill.service.IOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.seckill.service.ISeckillGoodsService;
import com.project.seckill.service.ISeckillOrderService;
import com.project.seckill.utils.MD5Util;
import com.project.seckill.utils.UUIDUtil;
import com.project.seckill.vo.GoodsVo;
import com.project.seckill.vo.OrderDetailVo;
import com.project.seckill.vo.RespBeanEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author ty
 * @since 2024-03-03
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    @Autowired
    private ISeckillGoodsService seckillGoodsService;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ISeckillOrderService seckillOrderService;
    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private GoodsMapper goodsMapper;
    @Transactional
    @Override
    public Order secKill(User user, GoodsVo goods) {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //查询商品
        SeckillGoods seckillGoods = seckillGoodsService.getOne(new QueryWrapper<SeckillGoods>().eq("goods_id", goods.getId()));
        //减库存
        seckillGoods.setStockCount(seckillGoods.getStockCount() - 1);

//        seckillGoodsService.updateById(seckillGoods);

        //查询商品
        boolean result = seckillGoodsService.update(
                new UpdateWrapper<SeckillGoods>()
                        .setSql("stock_count = " + "stock_count-1")
                        .eq("goods_id", goods.getId())
                        .gt("stock_count", 0));
//        判断是否有库存
        if(seckillGoods.getStockCount() < 1) {
            valueOperations.set("isStockEmpty:" + goods.getId(), "0");
            return null;
        }

        //生成订单
        Order order = new Order();
        order.setUserId(user.getId());
        order.setGoodsId(goods.getId());
        order.setDeliveryAddrId(0L);
        order.setGoodsName(goods.getGoodsName());
        order.setGoodsCount(1);
        order.setGoodsPrice(seckillGoods.getSeckillPrice());
        order.setOrderChannel(1);
        order.setStatus(0);
        order.setCreateDate(new Date());
        orderMapper.insert(order);
        //生成秒杀订单
        SeckillOrder seckillOrder = new SeckillOrder();
        seckillOrder.setUserId(user.getId());
        seckillOrder.setOrderId(order.getId());
        seckillOrder.setGoodsId(goods.getId());
        seckillOrderService.save(seckillOrder);
        redisTemplate.opsForValue().set("order:" + user.getId() + ":" + goods.getId(), seckillOrder);

        return order;
    }

    @Override
    public OrderDetailVo detail(Long orderId) {

        if(orderId == null) {
            throw new GlobalException(RespBeanEnum.ORDER_NOT_EXIST);
        }
        Order order = orderMapper.selectById(orderId);
        GoodsVo goodsVO = (GoodsVo) goodsService.findGoodsVoByGoodsId(order.getGoodsId());
        OrderDetailVo orderDetailVO = new OrderDetailVo();
        orderDetailVO.setOrder(order);
        orderDetailVO.setGoodsVO(goodsVO);
        return orderDetailVO;
    }

    @Override
    public String createPath(User user, Long goodsId) {
        String s = MD5Util.md5(UUIDUtil.uuid() + "123456");
        redisTemplate.opsForValue().set("seckillPath:" + user.getId() + ":" + goodsId, s, 60, TimeUnit.SECONDS);
        return s;
    }

    @Override
    public boolean checkPath(User user, Long goodsId, String path) {
        if(user == null ||goodsId<0|| StringUtils.isEmpty(path)) return false;
        String redisPath = (String) redisTemplate.opsForValue().get("seckillPath:" + user.getId() + ":" + goodsId);
        return path.equals(redisPath);
    }

    @Override
    public boolean checkCaptcha(User user, Long goodsId, String captcha) {
        if (StringUtils.isEmpty(captcha) || null == user || goodsId < 0) return false;
        String redisCaptcha = (String) redisTemplate.opsForValue().get("captcha:" + user.getId() + ":" + goodsId);
        return redisCaptcha.equals(captcha);
    }


}
