package com.atguigu.study.controller;



import com.alibaba.cloud.ai.dashscope.agent.DashScopeAgent;
import com.alibaba.cloud.ai.dashscope.agent.DashScopeAgentOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BaiLianRAGController {


    @Value("${spring.ai.dashscope.agent.options.app-id}")
    private String appId;

    private DashScopeAgent dashScopeAgent;

    public BaiLianRAGController(DashScopeAgent agentApi) {
        this.dashScopeAgent = agentApi;
    }

    @GetMapping("/bailian/chat")
    public String chat(@RequestParam(name = "msg", defaultValue = "北京") String message) {
        DashScopeAgentOptions build = DashScopeAgentOptions.builder().appId(appId).build();
        Prompt prompt = new Prompt(message,build);
        return dashScopeAgent.call(prompt).getResult().getOutput().getText();
    }


}
