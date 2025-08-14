package com.rites.sample.adk_samples.basic_agent;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;


public class BasicAgent {

    // ROOT_AGENT needed for ADK Web UI.
    public static BaseAgent ROOT_AGENT = initBasicAgent();
    public static String APP_NAME = "Basic Agent";
    public static String USER_ID = "test-user";

    public static void main(String[] args) {

        // If you wish to test your agent with CLI
        final InMemoryRunner runner = new InMemoryRunner(ROOT_AGENT);
        final Session session = runner
                .sessionService()
                .createSession(APP_NAME, USER_ID)
                .blockingGet();

        // Just for logging
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nExiting Basic Assistant. Goodbye!");
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
                Flowable<Event> events = runner.runAsync(USER_ID, session.id(), content);
                System.out.print("\nAgent > ");
                events.blockingForEach(event -> System.out.println(event.stringifyContent()));
            }
        }
    }

    public static BaseAgent initBasicAgent() {
        return LlmAgent.builder()
                .name("Basic Agent") // This name and the name in session should match
                .description("Greeting Agent")
                .model("gemini-2.0-flash-lite")
                .instruction("""
                        You are a helpful assistant that greets the user.
                        Ask for user's name and greet them by name.
                        """)
                .build();
    }
}
