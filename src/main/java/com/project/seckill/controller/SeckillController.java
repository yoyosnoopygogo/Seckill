package com.project.seckill.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.project.seckill.config.AccessLimit;
import com.project.seckill.exception.GlobalException;
import com.project.seckill.pojo.Order;
import com.project.seckill.pojo.SeckillMessage;
import com.project.seckill.pojo.SeckillOrder;
import com.project.seckill.pojo.User;
import com.project.seckill.rabbitmq.MQSender;
import com.project.seckill.service.*;
import com.project.seckill.utils.JsonUtil;
import com.project.seckill.vo.GoodsVo;
import com.project.seckill.vo.RespBean;
import com.project.seckill.vo.RespBeanEnum;
import com.wf.captcha.ArithmeticCaptcha;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Controller
@RequestMapping("/secKill")
public class SeckillController implements InitializingBean {
    @Autowired
    public IGoodsService goodsService;

    @Autowired
    public ISeckillOrderService seckillOrderService;
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    public IOrderService orderService;

    @Autowired
    public MQSender mqSender;
    private Map<Long,Boolean> emptyStockMap = new HashMap<>();

    @Autowired
    private GuavaRateLimiterService rateLimiterService;

    @RequestMapping("/doSecKill2")
    public String doSeckill2(Model model, User user, Long goodsId)
    {
        if(user==null)
        {
            return "login";
        }
        model.addAttribute("user",user);
        GoodsVo goods=(GoodsVo)goodsService.findGoodsVoByGoodsId(goodsId);
        if(goods.getStockCount()<1)
        {
            model.addAttribute("errmsg", RespBeanEnum.EMPTY_STOCK_ERROR.getMessage());
            return "secKillFail";
        }
        //判断订单(是否重复抢购)
        SeckillOrder seckillOrder = seckillOrderService.getOne(new QueryWrapper<SeckillOrder>().eq("user_id", user.getId()).eq("goods_id", goodsId));

        if(seckillOrder != null) {
            model.addAttribute("errmsg", RespBeanEnum.REPEAT_ERROR.getMessage());
            return "secKillFail";
        }
        //抢购
        Order order = orderService.secKill(user, goods);
        model.addAttribute("order", order);
        model.addAttribute("goods", goods);
        return "orderDetail";
    }

    @RequestMapping(value="/doSecKill",method = RequestMethod.POST)
    @ResponseBody
    public RespBean doSeckill1(Model model, User user, Long goodsId)
    {
        if(user==null)
        {
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }

        ValueOperations valueOperations=redisTemplate.opsForValue();
        //判断是否重复
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodsId);
        if(seckillOrder != null) {
            return RespBean.error(RespBeanEnum.REPEAT_ERROR);
        }
        if(emptyStockMap.get(goodsId))
        {
            return RespBean.error(RespBeanEnum.EMPTY_STOCK_ERROR);
        }
        //预减库存
        Long stock = valueOperations.decrement("seckillGoods:" + goodsId);
//        Long stock=(Long)redisTemplate.execute(redisScript, Collections.singletonList("seckillGoods:" + goodsId), Collections.EMPTY_LIST);
        if(stock < 0) {
            emptyStockMap.put(goodsId, true);
            valueOperations.increment("seckillGoods:" + goodsId);
            return RespBean.error(RespBeanEnum.EMPTY_STOCK_ERROR);
        }
        SeckillMessage seckillMessage=new SeckillMessage(user,goodsId);
        mqSender.sendSeckillMessage(JsonUtil.object2JsonStr(seckillMessage));
        return RespBean.success(0);
//        //抢购
//        Order order = orderService.secKill(user, goods);
//
//        return RespBean.success(order);
        /*
        GoodsVo goods=(GoodsVo)goodsService.findGoodsVoByGoodsId(goodsId);
        if(goods.getStockCount()<1)
        {
            model.addAttribute("errmsg", RespBeanEnum.EMPTY_STOCK_ERROR.getMessage());
            return RespBean.error(RespBeanEnum.EMPTY_STOCK_ERROR);
        }
        //判断订单(是否重复抢购)
//        SeckillOrder seckillOrder = seckillOrderService.getOne(new QueryWrapper<SeckillOrder>().eq("user_id", user.getId()).eq("goods_id", goodsId));
//
//        if(seckillOrder != null) {
//            model.addAttribute("errmsg", RespBeanEnum.REPEAT_ERROR.getMessage());
//            return RespBean.error(RespBeanEnum.REPEAT_ERROR);
//        }

        //判断是否重复
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodsId);
        if(seckillOrder != null) {
            return RespBean.error(RespBeanEnum.REPEAT_ERROR);
        }
        //抢购
        Order order = orderService.secKill(user, goods);

        return RespBean.success(order);
        */
//        return null;

    }
    @RequestMapping(value="{path}/doSecKill",method = RequestMethod.POST)
    @ResponseBody
    public RespBean doSeckill(@PathVariable String path, User user, Long goodsId)
    {
        if(user==null)
        {
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }

        ValueOperations valueOperations=redisTemplate.opsForValue();
        boolean check=orderService.checkPath(user,goodsId,path);

        if(!check)
        {
            return RespBean.error(RespBeanEnum.REQUEST_ILLEGAL);
        }

        //判断是否重复
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodsId);
        if(seckillOrder != null) {
            return RespBean.error(RespBeanEnum.REPEAT_ERROR);
        }
        if(emptyStockMap.get(goodsId))
        {
            return RespBean.error(RespBeanEnum.EMPTY_STOCK_ERROR);
        }
        //预减库存
        Long stock = valueOperations.decrement("seckillGoods:" + goodsId);
//        Long stock=(Long)redisTemplate.execute(redisScript, Collections.singletonList("seckillGoods:" + goodsId), Collections.EMPTY_LIST);
        if(stock < 0) {
            emptyStockMap.put(goodsId, true);
            valueOperations.increment("seckillGoods:" + goodsId);
            return RespBean.error(RespBeanEnum.EMPTY_STOCK_ERROR);
        }
        SeckillMessage seckillMessage=new SeckillMessage(user,goodsId);
        mqSender.sendSeckillMessage(JsonUtil.object2JsonStr(seckillMessage));
        return RespBean.success(0);

    }

    /*
    系统初始化，将商品库存数量加载到redis
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> list = goodsService.findGoodsVo();
        if(CollectionUtils.isEmpty(list)) return;
        list.forEach(goodsVO ->{
            redisTemplate.opsForValue().set("seckillGoods:" + goodsVO.getId(), goodsVO.getStockCount());
            emptyStockMap.put(goodsVO.getId(), false);
        });
    }

    @RequestMapping(value = "/result", method = RequestMethod.GET)
    @ResponseBody
    public RespBean getResult(User user, Long goodsId) {
        if(user == null) return RespBean.error(RespBeanEnum.SESSION_ERROR);
        Long orderId = seckillOrderService.getResult(user, goodsId);
        return RespBean.success(orderId);
    }

//    @AccessLimit(second=5,maxCount=5,needLogin=true)

    @RequestMapping(value = "/path", method = RequestMethod.GET)
    @ResponseBody
    public RespBean getPath(User user, Long goodsId, String captcha, HttpServletRequest request) {
        if (user == null) return RespBean.error(RespBeanEnum.SESSION_ERROR);
        if(!rateLimiterService.tryAcquire()){
            return RespBean.error(RespBeanEnum.ACCESS_LIMIT_REACHED);
        }
//        return ResultUtil.success1(1002,"未获取到许可");

        //访问限流，5s内访问5次
//        ValueOperations  valueOperations= redisTemplate.opsForValue();
//        String uri = request.getRequestURI();
//        Integer count = (Integer) valueOperations.get(uri + ":" + user.getId());
//        if(count == null) {
//            valueOperations.set(uri + ":" + user.getId(), 1, 5, TimeUnit.SECONDS);
//        } else if(count < 5) {
//            valueOperations.increment(uri + ":" + user.getId());
//        } else {
//            return RespBean.error(RespBeanEnum.ACCESS_LIMIT_REACHED);
//        }
        //验证码校验
        boolean check = orderService.checkCaptcha(user, goodsId, captcha);
        if(!check) return RespBean.error(RespBeanEnum.CAPTCHA_ERROR);

        String str = orderService.createPath(user,goodsId);
        return RespBean.success(str);
    }
    @RequestMapping(value = "/captcha", method = RequestMethod.GET)
    public void verifyCode(User user, Long goodsId, HttpServletResponse response) {
        if (null==user||goodsId<0){
            throw new GlobalException(RespBeanEnum.REQUEST_ILLEGAL);
        }
        // 设置请求头为输出图片类型
        response.setContentType("image/jpg");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        //生成验证码，将结果放入redis
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(130, 32, 3);
        redisTemplate.opsForValue().set("captcha:" + user.getId() + ":" + goodsId, captcha.text(),300, TimeUnit.SECONDS);
        try {
            captcha.out(response.getOutputStream());
        } catch (IOException e) {
            log.error("验证码生成失败", e.getMessage());
        }
    }
}
