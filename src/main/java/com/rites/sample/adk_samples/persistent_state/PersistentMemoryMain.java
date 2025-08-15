package com.rites.sample.adk_samples.persistent_state;

import com.google.adk.agents.BaseAgent;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.runner.Runner;
import com.google.adk.sessions.Session;
import com.google.common.collect.ImmutableList;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.rites.sample.adk_samples.persistent_state.AgentExecutorUtil.executeAgent;
import static com.rites.sample.adk_samples.persistent_state.PersistentMemoryAgent.initMemoryAgent;

public class PersistentMemoryMain {

    // ROOT_AGENT needed for ADK Web UI.
    public static BaseAgent ROOT_AGENT = initMemoryAgent();
    public static String APP_NAME = "memory_agent";
    public static String USER_NAME = "Mike-123";

    /**
     * Assume this method as an API call where the agent is running.
     * This API call with come with userId 'Mike-123'.
     * @param args
     */
    public static void main(String[] args) {
        System.out.println("Initializing ...");

        /* Step 1: Initialize the in-memory runner. DB runner is not available in Java for ADK */
        final InMemoryRunner runner = new InMemoryRunner(ROOT_AGENT);

        /* Step 2: Fetch existing or create a new session for user Mike */
        Session session = fetchOrCreateSession(runner, USER_NAME);

        /* Step 3: CLI */
        System.out.println("Welcome to Memory Agent Chat");
        System.out.println("Your reminders will be remembered across conversations");
        System.out.println("Type 'exit' to end the conversation");

        // Get the input from Console
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            while (true) {
                System.out.print("\nYou > ");
                String userInput = scanner.nextLine();
                if ("exit".equalsIgnoreCase(userInput)) {
                    break;
                }

                final Content content = Content.fromParts(Part.fromText(userInput));
                executeAgent(runner, USER_NAME, session, content);
            }
        }
    }



    private static Session fetchOrCreateSession(Runner runner, String userName) {
        // Fetch user session
        final ImmutableList<Session> sessions = runner.sessionService()
                .listSessions(APP_NAME, userName)
                .blockingGet()
                .sessions();
        Session session;

        if (sessions.isEmpty()) {
            /* Step 1: Fetch the initiate state for user Mike */
            final ConcurrentMap<String, Object> initialState = fetchStateOfUser(userName);

            /* Step 2: create session */
            System.out.println(String.format("Creating new session for user: %s", userName));
            session = runner.sessionService()
                    .createSession(APP_NAME, userName, initialState, null)
                    .blockingGet();
            System.out.println(String.format("Created new session: %s", session.id()));
        } else {
            session = sessions.get(0);
            System.out.println(String.format("Continuing with existing session: %s", session.id()));
        }
        return session;
    }

    private static ConcurrentMap<String, Object> fetchStateOfUser(String userName) {
        // This state can be built using some DB queries
        return new ConcurrentHashMap<>(
                Map.of("user_name", userName,
                        "reminders", new ArrayList<String>())
        );
    }
}
