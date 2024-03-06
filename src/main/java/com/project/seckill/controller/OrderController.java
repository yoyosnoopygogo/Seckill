package com.project.seckill.controller;


import com.project.seckill.pojo.User;
import com.project.seckill.service.IOrderService;
import com.project.seckill.vo.OrderDetailVo;
import com.project.seckill.vo.RespBean;
import com.project.seckill.vo.RespBeanEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author ty
 * @since 2024-02-20
 */
@Controller
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private IOrderService orderService;

    /***
     * 订单详情
     * @param user
     * @param orderId
     * @return
     */
    @RequestMapping("/detail")
    @ResponseBody
    public RespBean detail(User user, Long orderId) {
        if(user == null) return RespBean.error(RespBeanEnum.SESSION_ERROR);
        OrderDetailVo orderDetailVO = orderService.detail(orderId);
        return RespBean.success(orderDetailVO);
    }
}
