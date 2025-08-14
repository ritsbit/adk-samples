package com.rites.sample.adk_samples.structured_output;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import io.reactivex.rxjava3.core.Flowable;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;

public class StructuredOutputAgent {
    // ROOT_AGENT needed for ADK Web UI.
    public static BaseAgent ROOT_AGENT = initToolAgent();

    public static BaseAgent initToolAgent() {
        return LlmAgent.builder()
                .name("email_agent") // it's a good practice to name your agent with UNDERSCORE
                .description("Generates professional emails with structured subject and body")
                .model("gemini-2.0-flash-lite")
                .instruction("""
                        You are an Email Generation Assistant.
                        Your task is to generate professional email based on the user's request.
                        
                        GUIDELINES:
                        - Create an appropriate subject line (concise and relevant)
                        - Write a well structured email body with:
                            * Professional greeting
                            * clear and consice main content
                            * appropriate closing
                            * your name as signature
                        - Suggest relevant attachments if applicable (empty list if none needed)
                        - Email tone should match the purpose (formal for business, casual for colleagues)
                        - Keep email concise but complete
                        
                        IMPORTANT: Your response MUST be valid JSON matching this structure
                        {
                            "subject": "Subject line here",
                            "body": "Email body here with proper paragraphs and formatting"
                        }
                        
                        DO NOT include any explanations or additional text outside the JSON response.
                        """)
                .outputKey("email")
                .outputSchema(EMAIL_SCHEMA)
                // Control whether the agent receives the prior conversation history.
                .includeContents(LlmAgent.IncludeContents.NONE)
                .build();
    }

    private static final Schema EMAIL_SCHEMA =
            Schema.builder()
                    .type("OBJECT")
                    .description("Schema for email's subject and body")
                    .properties(
                            Map.of(
                                    "subject", Schema.builder().type("STRING").description("The subject line of the email").build(),
                                    "body", Schema.builder().type("STRING").description("body of the email").build()

                    ))
                    .build();

    public static void main(String[] args) {

        // If you wish to test your agent with CLI
        final InMemoryRunner runner = new InMemoryRunner(ROOT_AGENT);
        final Session session = runner
                .sessionService()
                .createSession("email_agent", "test-user")
                .blockingGet();

        // Just for logging
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nExiting Email Assistant. Goodbye!");
        }));

        // Get the input from Console
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            while (true) {
                System.out.print("\nYou > ");
                String userInput = scanner.nextLine();
                if ("quit".equalsIgnoreCase(userInput)) {
                    break;
                }

                final Content content = Content.fromParts(Part.fromText(userInput));
                Flowable<Event> events = runner.runAsync("test-user", session.id(), content);
                System.out.print("\nAgent > ");
                events.blockingForEach(event -> System.out.println(event.stringifyContent()));
            }
        }
    }
}
