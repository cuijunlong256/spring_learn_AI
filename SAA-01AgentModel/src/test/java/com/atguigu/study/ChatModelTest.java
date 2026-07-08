package com.atguigu.study;

import com.alibaba.cloud.ai.dashscope.api.DashScopeImageApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageModel;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ChatModelTest {

    @Test
    public void testDashCaht01(@Autowired DashScopeChatModel dashScopeChatModel) {
        String call = dashScopeChatModel.call("你好");
        System.out.println(call);
    }


    @Test
    public void testDashCaht02(@Autowired DashScopeImageModel dashScopeChatModel) {
        DashScopeImageOptions build = DashScopeImageOptions.builder().withModel("wanx2.1-t2i-turbo").build();
        ImageResponse imageResponse = dashScopeChatModel.call(new ImagePrompt("程序员吕布", build));
        String url = imageResponse.getResult().getOutput().getUrl();
        System.out.println(url);
    }

}
