package com.project.seckill.service;

import com.project.seckill.pojo.Goods;
import com.baomidou.mybatisplus.extension.service.IService;
import com.project.seckill.vo.GoodsVo;
import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author ty
 * @since 2024-03-03
 */

public interface IGoodsService extends IService<Goods> {

    List<GoodsVo> findGoodsVo();

    GoodsVo findGoodsVoByGoodsId(Long goodsId);
}
