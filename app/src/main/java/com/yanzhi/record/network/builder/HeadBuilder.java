package com.yanzhi.record.network.builder;

import com.yanzhi.record.network.OkHttpUtils;
import com.yanzhi.record.network.request.OtherRequest;
import com.yanzhi.record.network.request.RequestCall;

/**
 * Created by zhy on 16/3/2.
 */
public class HeadBuilder extends GetBuilder {
    @Override
    public RequestCall build() {
        return new OtherRequest(null, null, OkHttpUtils.METHOD.HEAD, url, tag, params, headers, id).build();
    }
}
