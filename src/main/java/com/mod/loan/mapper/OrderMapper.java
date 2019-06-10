package com.mod.loan.mapper;

import com.mod.loan.common.mapper.MyBaseMapper;
import com.mod.loan.model.Order;
import org.apache.ibatis.annotations.Param;

public interface OrderMapper extends MyBaseMapper<Order> {

    int countOrderPaySuccessOneDay(@Param("uid") Long uid);
}