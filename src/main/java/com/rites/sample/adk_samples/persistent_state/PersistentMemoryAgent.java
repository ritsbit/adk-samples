package com.rites.sample.adk_samples.persistent_state;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.sessions.State;
import com.google.adk.tools.Annotations;
import com.google.adk.tools.FunctionTool;
import com.google.adk.tools.ToolContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PersistentMemoryAgent {

    public static BaseAgent initMemoryAgent() {
        return LlmAgent.builder()
                .name("memory_agent")
                .description("A smart reminder assistant with persistent memory")
                .model("gemini-2.0-flash-lite")
                .instruction("""
                        You are a friendly reminder assistant that remembers users across conversations.
                        if do not find user_name then use add_user_name tool to add it to the state.
                        
                        The user's information is stored in state:
                        - User's name: {user_name}
                        - Reminders: {reminders}
                        
                        Always be friendly and address the user by name. If you don't know their name yet,
                        use the update_user_name tool to store it when they introduce themselves.
                        
                            You can help users manage their reminders with the following capabilities:
                            1. Add new reminders
                            2. View existing reminders
                            3. Update reminders
                            4. Delete reminders
                            5. Update the user's name
                        
                            **REMINDER MANAGEMENT GUIDELINES:**
                        
                            When dealing with reminders, you need to be smart about finding the right reminder:
                        
                            1. When the user asks to update or delete a reminder but doesn't provide an index:
                               - If they mention the content of the reminder (e.g., "delete my meeting reminder"),\s
                                 look through the reminders to find a match
                               - If you find an exact or close match, use that index
                               - Never clarify which reminder the user is referring to, just use the first match
                               - If no match is found, list all reminders and ask the user to specify
                        
                            2. When the user mentions a number or position:
                               - Use that as the index (e.g., "delete reminder 2" means index=2)
                               - Remember that indexing starts at 1 for the user
                        
                            3. For relative positions:
                               - Handle "first", "last", "second", etc. appropriately
                               - "First reminder" = index 1
                               - "Last reminder" = the highest index
                               - "Second reminder" = index 2, and so on
                        
                            4. For viewing:
                               - Always use the view_reminders tool when the user asks to see their reminders
                               - Format the response in a numbered list for clarity
                               - If there are no reminders, suggest adding some
                        
                            5. For addition:
                               - Extract the actual reminder text from the user's request
                               - Remove phrases like "add a reminder to" or "remind me to"
                               - Focus on the task itself (for example, "add a reminder to buy milk" → add_reminder("buy milk"))
                        
                            6. For updates:
                               - Identify both which reminder to update and what the new text should be
                               - For example, "change my second reminder to pick up groceries" → update_reminder(2, "pick up groceries")
                        
                            7. For deletions:
                               - Confirm deletion when complete and mention which reminder was removed
                               - For example, "I've deleted your reminder to 'buy milk'"
                        
                            Remember to explain that you can remember their information across conversations.
                        
                            IMPORTANT:
                            - use your best judgement to determine which reminder the user is referring to.\s
                            - You don't have to be 100% correct, but try to be as close as possible.
                            - Never ask the user to clarify which reminder they are referring to.
                        """)
                .tools(List.of(
                        FunctionTool.create(PersistentMemoryAgent.class, "addReminder"),
                        FunctionTool.create(PersistentMemoryAgent.class, "viewReminders"),
                        FunctionTool.create(PersistentMemoryAgent.class, "updateReminder"),
                        FunctionTool.create(PersistentMemoryAgent.class, "deleteReminder"),
                        FunctionTool.create(PersistentMemoryAgent.class, "addUserName")
                ))
                .build();
    }

    @Annotations.Schema(name = "update_reminder",
            description = "Updates an existing reminder")
    public static Map<String, Object> updateReminder(
            @Annotations.Schema(description = "The 1-based index of the reminder to update") int index,
            @Annotations.Schema(description = "The new text for the reminder") String reminderText,
            @Annotations.Schema(description = "Context for accessing and updating session state") ToolContext toolContext) {

        final List<String> reminders = getRemindersFromState(toolContext);
        if (reminders.isEmpty() || index < 0 || index-1 >= reminders.size()) {
            return Map.of("action", "update_reminder",
                    "status", "error",
                    "message", "Could not find reminder at index: " + index);
        }
        reminders.add(index-1, reminderText);
        reminders.remove(index);
        toolContext.state().put("reminders", reminders);
        return Map.of("action", "update_reminder",
                "index", index,
                "updated_text", reminderText,
                "message", "Reminder Updated successfully");
    }

    @Annotations.Schema(name = "delete_reminder",
            description = "deletes an existing reminder")
    public static Map<String, Object> deleteReminder(
            @Annotations.Schema(description = "The 1-based index of the reminder to delete") int index,
            @Annotations.Schema(description = "Context for accessing and updating session state") ToolContext toolContext) {

        final List<String> reminders = getRemindersFromState(toolContext);
        if (reminders.isEmpty() || index < 0 || index-1 >= reminders.size()) {
            return Map.of("action", "delete_reminder",
                    "status", "error",
                    "message", "Could not find reminder at index: " + index);
        }
        reminders.remove(index-1);
        toolContext.state().put("reminders", reminders);
        return Map.of("action", "delete_reminder",
                "index", index,
                "message", "Reminder deleted successfully");
    }


    @Annotations.Schema(name = "add_reminder",
            description = "Add a new reminder to the user's reminder list")
    public static Map<String, Object> addReminder(
            @Annotations.Schema(description = "The reminder text to add") String reminder,
            @Annotations.Schema(description = "Context for accessing and updating session state") ToolContext toolContext) {

        State state = toolContext.state();
        List<String> reminders = getRemindersFromState(toolContext);
        reminders.add(reminder);
        state.put("reminders", reminders);

        System.out.println(String.format("Add reminder tool called for user: %s", state.get("user_name")));
        return Map.of("action", "add_reminder",
                "reminder", reminder,
                "message", "Reminder added successfully");
    }

    @Annotations.Schema(name = "view_reminders",
            description = "View all current reminders")
    public static Map<String, Object> viewReminders(
            @Annotations.Schema(description = "Context for accessing and updating session state") ToolContext toolContext) {
        List<String> reminders = getRemindersFromState(toolContext);

        System.out.println(String.format("view_reminders tool called for user: %s", toolContext.state().get("user_name")));
        return Map.of("action", "view_reminders",
                "reminders", reminders,
                "count", reminders.size());
    }

    @Annotations.Schema(name = "add_user_name",
            description = "Add user_name to the state")
    public static Map<String, Object> addUserName(
            @Annotations.Schema(description = "The username to add") String userName,
            @Annotations.Schema(description = "Context for accessing and updating session state") ToolContext toolContext) {

        State state = toolContext.state();
        state.put("user_name", userName);

        System.out.println(String.format("view_reminders tool called for user: %s", state.get("user_name")));
        return Map.of("action", "add_user_name",
                "user_name", userName);
    }

    private static List<String> getRemindersFromState(ToolContext toolContext) {
        State state = toolContext.state();
        List<String> reminders = (List<String>) state.getOrDefault("reminders", new ArrayList<String>());
        return reminders;
    }
}
