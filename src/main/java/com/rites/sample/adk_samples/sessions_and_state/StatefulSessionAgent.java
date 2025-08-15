package com.rites.sample.adk_samples.sessions_and_state;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class StatefulSessionAgent {

    // ROOT_AGENT needed for ADK Web UI.
    public static BaseAgent ROOT_AGENT = initToolAgent();

    private static BaseAgent initToolAgent() {
        return LlmAgent.builder()
                .name("question_answer_agent")
                .description("Question answer agent")
                .model("gemini-2.0-flash-lite")
                .instruction("""
                        You are a helpful answers questions about the user's preferences.
                        
                        Here is the some information about the user:
                        Name:
                        {user_name}
                        Preferences:
                        {user_preferences}
                        """)
                .build();
    }

    public static void main(String[] args) {
        final ConcurrentMap<String, Object> initiateState = new ConcurrentHashMap(
                Map.of("user_name", "Mike",
                "user_preferences", """
                        I like to play pickleball, Golf and Tennis.
                        My favourite food is italian.
                        My favourite TV show is Game of Thrones.
                        Loves it when people do not watch reels.
                        """));

        System.out.println("Initializing ...");
        final InMemoryRunner runner = new InMemoryRunner(ROOT_AGENT);
        final Session session = runner
                .sessionService()
                .createSession("question_answer_agent", "Mike-userId", initiateState, null)
                .blockingGet();
        System.out.println(String.format("Initialized and created session with ID: %s", session.id()));

        // Just for logging
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nExiting Assistant. Goodbye!");
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
                Flowable<Event> events = runner.runAsync("Mike-userId", session.id(), content);
                System.out.print("\nAgent Thinking ... > ");
                events.blockingForEach(event -> System.out.println(event.stringifyContent()));
            }
        }
    }
}
