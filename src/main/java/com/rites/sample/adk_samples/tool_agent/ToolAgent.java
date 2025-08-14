package com.rites.sample.adk_samples.tool_agent;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.Annotations;
import com.google.adk.tools.FunctionTool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class ToolAgent {

    // ROOT_AGENT needed for ADK Web UI.
    public static BaseAgent ROOT_AGENT = initToolAgent();

    @Annotations.Schema(name = "get_current_time",
            description = "Get the current time in the format YYYY-MM-DD HH:MM:SS")
    public static Map<String, Object> getCurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        String pattern = "yyyy-MM-dd HH:MM:SS";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

        // Always return MAP from your tool so that LLM can better understand it.
        // It's a good practice to always return the result of the tool
        return Map.of("current_time", now.format(formatter),
                "status", "success");
    }

    public static BaseAgent initToolAgent() {
        // Create the FunctionTool from the Java method
        FunctionTool getCurrentTimeTool = FunctionTool.create(ToolAgent.class, "getCurrentTime");

        return LlmAgent.builder()
                .name("tool_agent")
                .description("Tool Agent")
                .model("gemini-2.0-flash-lite")
                .instruction("""
                        You are a helpful assistant that can use following tool:
                        - get_current_time
                        """)
//                .tools(new GoogleSearchTool())
                .tools(getCurrentTimeTool)
                // the below is not possible. You can use ADK provided tool in isolation but ADK provided tool cannot be clubbed with custom tools
//                .tools(getCurrentTimeTool, new GoogleSearchTool())
                .build();
    }
}
