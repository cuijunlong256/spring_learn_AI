package com.atguigu.study.service;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import javax.validation.constraints.NotNull;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
public class RemoteMcpToolsService {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(RemoteMcpToolsService.class);

    private final ToolCallbackProvider toolCallbackProvider;

    public RemoteMcpToolsService(ToolCallbackProvider toolCallbackProvider) {
        this.toolCallbackProvider = toolCallbackProvider;
    }

    public static @NotNull ChatModel getChatModel() {
        DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("aiQwen_key")).build();
        return DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
    }


    public String remoteMcpToolsReactWithSpringBootExample(String input) throws GraphRunnerException {
        ChatModel chatModel = getChatModel();
        ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();
        logger.info("toolCallbacks: {}", JSON.toJSONString(toolCallbacks));
        String instruction = "You are a helpful assistant that can call remote mcp tools.";
        return executeAgent(chatModel, instruction, "travel_planning_session", input, toolCallbackProvider, toolCallbacks);
    }

    /**
     * 示例18：解耦 Spring Boot 使用远端MCP工具 -- React Agent
     *
     * <p>不使用 Spring 依赖注入的情况下，直接使用 MCP 客户端
     * 获取远程工具，并将其转换为 Spring AI 的 ToolCallback，最后在 ReactAgent 中使用。</p>
     *
     * <p>关键步骤：</p>
     * <ol>
     *   <li>创建 MCP 客户端传输层 (HttpClientSseClientTransport)</li>
     *   <li>构建并初始化 MCP 同步客户端</li>
     *   <li>调用 listTools() 获取远程服务器的工具列表</li>
     *   <li>将 MCP 工具转换为 Spring AI 的 ToolCallback</li>
     * </ol>
     */
    public String remoteMcpToolsReactWithoutSpringBootExample(String message) throws GraphRunnerException {
        ChatModel chatModel = getChatModel();

        String modelScope12306BaseUrlSse = System.getenv("MODEL_SCOPE_12306_BASE_URL");
        String modelScopeAmapBaseUrlSse = System.getenv("MODEL_SCOPE_AMAP_BASE_URL");
        HttpClient.Builder httpBuilder = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofSeconds(60));
        List<ToolCallback> toolCallbacks = new ArrayList<>();
        List<McpSyncClient> clientsToClose = new ArrayList<>();
        try {
            if (modelScope12306BaseUrlSse != null && !modelScope12306BaseUrlSse.isBlank()) {
                List<ToolCallback> tools12306 = fetchMcpTools(
                        modelScope12306BaseUrlSse,
                        "sse",
                        httpBuilder,
                        clientsToClose,
                        "12306", false
                );
                toolCallbacks.addAll(tools12306);
            } else {
                logger.warn("Environment variable MODEL_SCOPE_12306_BASE_URL is not set or empty, skipping 12306 MCP tools");
            }

            if (modelScopeAmapBaseUrlSse != null && !modelScopeAmapBaseUrlSse.isBlank()) {
                List<ToolCallback> toolsAmap = fetchMcpTools(
                        modelScopeAmapBaseUrlSse,
                        "mcp",
                        httpBuilder,
                        clientsToClose,
                        "amap", true
                );
                toolCallbacks.addAll(toolsAmap);
            } else {
                logger.warn("Environment variable MODEL_SCOPE_AMAP_BASE_URL is not set or empty, skipping amap MCP tools");
            }
            return executeAgent(chatModel, "你是一个图片专家，可以帮助查找对应的信息", "travel_planning_session_no_spring", message, null, toolCallbacks.toArray(new ToolCallback[0]));
        } catch (Exception e) {
            logger.error("execute MCP Agent error", e);
            return "Error executing MCP Agent";
        } finally {
//close all MCP client
            for (McpSyncClient client : clientsToClose) {
                try {
                    if (client != null) {
                        client.close();
                        logger.info("MCP Client Is Closed");
                    }
                } catch (Exception e) {
                    logger.warn("Close MCP Client Error", e);
                }
            }
        }

    }

    public List<ToolCallback> fetchMcpTools(String baseUrl,
                                            String endpoint,
                                            HttpClient.Builder httpBuilder,
                                            List<McpSyncClient> clientsToClose,
                                            String serverName,
                                            boolean isStreamable) {
        List<ToolCallback> toolCallbacks = new ArrayList<>();
        McpSyncClient mcpClient;
        try{
            McpClientTransport mcpClientTransport;
            if (isStreamable) {
                ObjectMapper objectMapper = new ObjectMapper();
                mcpClientTransport = HttpClientStreamableHttpTransport
                        .builder(baseUrl)
                        .endpoint(endpoint)
                        .clientBuilder(HttpClient.newBuilder())
                        .jsonMapper(new JacksonMcpJsonMapper(objectMapper)).build();
            } else {
                mcpClientTransport = HttpClientSseClientTransport.builder(baseUrl)
                        .clientBuilder(httpBuilder)
                        .sseEndpoint(endpoint)
                        .build();
            }
            mcpClient = McpClient.sync(mcpClientTransport)
                    .requestTimeout(Duration.ofSeconds(60))
                    .initializationTimeout(Duration.ofSeconds(60))
                    .capabilities(McpSchema.ClientCapabilities.builder().roots(true).build())
                    .build();

            clientsToClose.add(mcpClient);
//initialize MCP client
            logger.info("[{}] Initialize MCP Client...", serverName);
            McpSchema.InitializeResult initResult = mcpClient.initialize();
            logger.info("[{}] MCP Client Initialize Successful: serverInfo={}, capabilities={}",
                    serverName, initResult.serverInfo(), initResult.capabilities());

            //get tools
            logger.info("[{}] Get Tools...", serverName);
            McpSchema.ListToolsResult toolsResult = mcpClient.listTools();
            List<McpSchema.Tool> mcpTools = toolsResult.tools();

            logger.info("[{}] Found {} Tools From MCP Server", serverName, mcpTools.size());

            for (McpSchema.Tool mcpTool : mcpTools) {
                ToolCallback toolCallback = createToolCallback(mcpTool, mcpClient, serverName);
                toolCallbacks.add(toolCallback);
            }
        }catch (Exception e){
            logger.error("[{}] Fetch MCP Tools Error", serverName, e);
        }
        return toolCallbacks;
    }

    private ToolCallback createToolCallback(McpSchema.Tool mcpTool, McpSyncClient mcpClient, String serverName){
        return FunctionToolCallback.builder(
                        mcpTool.name(),
                        (Map<String, Object> functionInput) -> {
                            try {
                                //build tools request
                                logger.debug("[{}] Call MCP Tool: {} With Input: {}",
                                        serverName, mcpTool.name(), functionInput);
                                McpSchema.CallToolRequest callRequest = new McpSchema.CallToolRequest(
                                        mcpTool.name(), functionInput);

                                // Call Tool
                                McpSchema.CallToolResult callResult = mcpClient.callTool(callRequest);

                                // get return
                                StringBuilder resultBuilder = new StringBuilder();
                                for (McpSchema.Content content : callResult.content()) {
                                    if (content instanceof McpSchema.TextContent textContent) {
                                        resultBuilder.append(textContent.text());
                                    }
                                }

                                String result = resultBuilder.toString();
                                logger.debug("[{}] MCP Tool Return: {}", serverName, result);
                                return result;

                            } catch (Exception e) {
                                logger.error("[{}] Call MCP Tool Failed: {}",
                                        serverName, mcpTool.name(), e);
                                return "{\"error\": \"" + e.getMessage() + "\"}";
                            }
                        })
                .description(mcpTool.description())
                .inputType(Map.class)
                .build();
    }

    public static String executeAgent(ChatModel chatModel, String instruction, String travel_planning_session, String message, ToolCallbackProvider toolCallbackProvider,
                                      ToolCallback... toolCallback) throws GraphRunnerException {
        Builder builder = ReactAgent.builder()
                .name("remote_mcp_tools")
                .model(chatModel)
                .description("A tool to call remote mcp tools")
                .instruction(instruction)
                .saver(new MemorySaver());

        if (toolCallbackProvider != null) {
            builder.toolCallbackProviders(toolCallbackProvider);
        }
        else {
            builder.tools(toolCallback);
        }
        ReactAgent agent = builder.build();
        RunnableConfig config = RunnableConfig
                .builder()
                .threadId(travel_planning_session)
                .build();
        Flux<NodeOutput> stream = agent.stream(message, config);
        StringBuilder answerString = new StringBuilder();
        stream.doOnNext(output -> {
            if (output.node().equals("_AGENT_MODEL_")) {
                answerString.append(((StreamingOutput<?>) output).message().getText());
            } else if (output.node().equals("_AGENT_TOOL_")){
                answerString.append("\nTool Call:").append(((ToolResponseMessage) ((StreamingOutput<?>) output).message()).getResponses().get(0)).append("\n");
            }
                })
                .doOnComplete(() -> logger.info("Agent response: {}", answerString))
                .doOnError(e -> logger.error("Stream Processing Error", e))
                .blockLast();

        return answerString.toString();
    }



    public String remoteMcpToolsWithChatCliAndSpringBootExample(String message) {
        ChatModel chatModel = getChatModel();
        ChatClient chatClient = ChatClient.builder(chatModel)
                .build();
        ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();
        logger.info("""
                        ==============================Find the tools from spring ToolCallbackProvider==============================
                        {}
                        """,
                JSON.toJSONString(toolCallbacks));
        ChatClient.ChatClientRequestSpec callbacks = chatClient
                .prompt("You are a helpful assistant.")
                .user(message)
                .toolCallbacks(toolCallbacks);

        return callbacks.call().chatResponse().getResult().getOutput().getText();
    }
}
