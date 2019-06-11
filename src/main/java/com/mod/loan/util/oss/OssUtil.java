package com.mod.loan.util.oss;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.OSSObject;
import com.mod.loan.config.Constant;

import java.io.InputStream;

public class OssUtil {

    /**
     * 获取身份证正反面
     */
    public static InputStream getCertImage(String filePath) {
        try {
            OSSClient ossClient = new OSSClient(endPointUrl(Constant.ENVIROMENT), Constant.OSS_ACCESSKEY_ID,
                    Constant.OSS_ACCESS_KEY_SECRET);
            OSSObject ossObject = ossClient.getObject(Constant.OSS_STATIC_BUCKET_NAME, filePath);
            return ossObject.getObjectContent();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据环境切换上传地址
     *
     * @param env
     * @return
     */
    private static String endPointUrl(String env) {
        if ("dev".equals(env)) {
            return Constant.OSS_ENDPOINT_OUT_URL;
        }
        return Constant.OSS_ENDPOINT_IN_URL;
    }

}
