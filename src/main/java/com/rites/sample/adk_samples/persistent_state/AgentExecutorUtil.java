package com.rites.sample.adk_samples.persistent_state;

import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.adk.sessions.State;
import com.google.genai.types.Content;
import io.reactivex.rxjava3.core.Flowable;

import java.util.ArrayList;
import java.util.List;

public class AgentExecutorUtil {
    public static void executeAgent(InMemoryRunner runner, String userName,
                                    Session session, Content content) {
        printState((State) session.state(), "State BEFORE processing");
        System.out.print("\nAgent Thinking ... > ");
        Flowable<Event> events = runner.runAsync(userName, session.id(), content);
        events.blockingForEach(event -> {
            System.out.println(event.stringifyContent());
            System.out.println("-----------------------------------");
        });
        printState((State) session.state(), "State AFTER processing");
    }

    private static void printState(final State state, String msg) {
        System.out.println(msg);
        List<String> reminders = (List<String>) state.getOrDefault("reminders", new ArrayList<String>());
        if (reminders != null && !reminders.isEmpty()) {
            reminders.forEach(System.out::println);
        }
    }
}
