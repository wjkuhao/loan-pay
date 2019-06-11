package com.mod.loan.util.kuaiqianutil.common;


import com.mod.loan.util.kuaiqianutil.notice.NotifyHead;
import com.mod.loan.util.kuaiqianutil.notice.NotifyResponse;
import com.mod.loan.util.kuaiqianutil.notice.NotifyResponseBody;
import com.mod.loan.util.kuaiqianutil.pay.Pay2bankHead;
import com.mod.loan.util.kuaiqianutil.pay.Pay2bankOrder;
import com.mod.loan.util.kuaiqianutil.pay.Pay2bankRequest;
import com.mod.loan.util.kuaiqianutil.pay.RequestBody;
import com.mod.loan.util.kuaiqianutil.query.Pay2bankSearchHead;
import com.mod.loan.util.kuaiqianutil.query.Pay2bankSearchRequest;
import com.mod.loan.util.kuaiqianutil.query.SearchRequestBody;


/**
 * 工具类
 *
 * @author zhiwei.ma
 */
public class CCSUtil {

    /**
     * 创建代付request
     * @return
     */
    public static Pay2bankRequest genPayRequest(String membercode_head , String version){
        Pay2bankRequest request = new Pay2bankRequest();
        Pay2bankHead head = new Pay2bankHead();
        head.setMemberCode(membercode_head);
        head.setVersion(version);
        RequestBody requestBody = new RequestBody();
        SealDataType sealDataType = new SealDataType();
        requestBody.setSealDataType(sealDataType);
        request.setPay2bankHead(head);
        request.setRequestBody(requestBody);
        return request;
    }

    /**
     * 创建代付查询request
     *
     * @return
     */
    public static Pay2bankSearchRequest genPayQueryRequest(String membercode_head, String version) {
        Pay2bankSearchRequest request = new Pay2bankSearchRequest();
        Pay2bankSearchHead head = new Pay2bankSearchHead();
        head.setMemberCode(membercode_head);
        head.setVersion(version);
        SearchRequestBody requestBody = new SearchRequestBody();
        SealDataType sealDataType = new SealDataType();
        requestBody.setSealDataType(sealDataType);
        request.setPay2bankSearchHead(head);
        request.setSearchRequestBody(requestBody);
        return request;
    }

    /**
     * 创建通知response
     * @param membercode_head
     * @param version
     * @return
     */
    public static NotifyResponse genNoticeResponse(String membercode_head , String version){
        NotifyResponse response = new NotifyResponse();
        NotifyHead head = new NotifyHead();
        head.setMemberCode(membercode_head);
        head.setVersion(version);
        NotifyResponseBody responseBody = new NotifyResponseBody();
        SealDataType sealDataType = new SealDataType();
        responseBody.setSealDataType(sealDataType);
        responseBody.setIsReceived("1");
        response.setNotifyHead(head);
        response.setNotifyResponseBody(responseBody);
        return response;
    }
}
