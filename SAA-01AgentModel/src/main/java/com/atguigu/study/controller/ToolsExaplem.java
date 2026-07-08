package com.atguigu.study.controller;




import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.agent.renderer.SaaStTemplateRenderer;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.template.TemplateRenderer;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.function.Function;

public class ToolsExaplem {


    /**
     * 示例1：编程方式规范 - FunctionToolCallback
     */
   public static void programmaticToolSpecification(){
       ToolCallback toolCallback = FunctionToolCallback.builder("currentWeather", new WeatherService()).
               description("Get the current weather in a given location")
               .inputType(WeatherRequest.class)
               .build();
   }

   public static void toolCall() throws GraphRunnerException {
       DashScopeApi dashScopeApi = DashScopeApi.builder()
               .apiKey(System.getenv("aiQwen_key"))
               .build();

       ChatModel chatModel = DashScopeChatModel.builder()
               .dashScopeApi(dashScopeApi)
               .build();
       ToolCallback toolCallback = FunctionToolCallback
               .builder("currentWeather", new WeatherService())
               .description("Get the weather in location")
               .inputType(WeatherRequest.class)
               .build();

       String systemPrompt = """
				你是一个专业的{{role}}助手。
				你的专业领域是{{domain}}。
				请用{{language}}语言回答用户的问题。
				""";

       RunnableConfig config = RunnableConfig.builder()
               .threadId("user_123")
               .build();

       // 使用自定义分隔符的 instruction
       String instruction = """
				用户询问的主题是：{{topic}}
				请根据以下要求回答：
				1. 保持专业性
				2. 提供具体示例
				3. 语言要{{style}}
				""";
       TemplateRenderer customRenderer = SaaStTemplateRenderer.builder()
               .startDelimiter("{{")
               .endDelimiter("}}")
               .build();


       ToolCallback accountTool = FunctionToolCallback
               .builder("get_account_info", new AccountInfoTool())
               .description("Get the current user's account information")
               .inputType(String.class)
               .build();


       ReactAgent agent = ReactAgent.builder()
               .name("custom_template_agent")
               .model(chatModel)
               .systemPrompt(systemPrompt)
               .instruction(instruction)
               .templateRenderer(customRenderer)
               .saver(new MemorySaver())
               .hooks(new SpringAATest.LoggingHook(), ModelCallLimitHook.builder().runLimit(3).build(),new SpringAATest.CustomStopConditionHook())
               .interceptors(new SpringAATest.GuardrailInterceptor(),new SpringAATest.ToolMonitoringInterceptor())
               .tools(toolCallback)
               .build();
       AssistantMessage call = agent.call("我叫张三", config);
       System.out.println(call.getText());
       AssistantMessage call1 = agent.call("我叫什么", config);
       System.out.println(call1.getText());

   }

    public static void main(String[] args) throws GraphRunnerException {
        // TODO Auto-generated method stub
        toolCall();

    }

    public enum Unit {C, F}

    // ==================== 访问上下文 ====================

    public enum UnitType {CELSIUS, FAHRENHEIT}

    public static class WeatherService implements Function<WeatherRequest, WeatherResponse> {
        @Override
        public WeatherResponse apply(WeatherRequest request) {
            return new WeatherResponse(30.0, Unit.C);
        }
    }

    public record WeatherRequest(@ToolParam(description = "城市或坐标") String location,
                                 Unit unit) { }

    public record WeatherResponse(double temp, Unit unit){
    }

}
