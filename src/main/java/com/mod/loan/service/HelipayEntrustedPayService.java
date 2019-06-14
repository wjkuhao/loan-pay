package com.mod.loan.service;

import com.mod.loan.model.Merchant;
import com.mod.loan.model.Order;
import com.mod.loan.model.User;
import com.mod.loan.model.UserBank;
import com.mod.loan.util.heliutil.vo.MerchantUserUploadResVo;
import com.mod.loan.util.heliutil.vo.OrderQueryResVo;
import com.mod.loan.util.heliutil.vo.OrderResVo;

public interface HelipayEntrustedPayService {

    MerchantUserUploadResVo bindUserCard(Long uid, String merchantAlias);

    OrderResVo bindUserCardPay(String payNo, User user, UserBank userBank, Order order, Merchant merchant);

    OrderResVo entrustedPay(String payNo, String amount, User user, UserBank userBank, Merchant merchant);

    OrderQueryResVo entrustedPayQuery(String payNo, UserBank userBank, Merchant merchant);

}
