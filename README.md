# adk-samples

# Steps to run any agent
1. export GOOGLE_API_KEY=<your key>
2. export GOOGLE_GENAI_USE_VERTEXAI=FALSE
3. Then, run the ADK web UI command. This will download Maven dependencies including ADK Java, compile the agent code, and start a development web server.
4. Use command
   > mvn compile exec:java "-Dexec.args=--server.port=8080 \
      --adk.agents.source-dir=src/ \
      --logging.level.com.google.adk.dev=DEBUG \
      --logging.level.com.google.adk.demo.agents=DEBUG"
5. Make sure to update the **MainClass** name in the **pom.xml**
6. Open http://localhost:8080 to test your agent

# Order you should follow to learn step-by-step
1. basic_agent
2. tool_agent
3. structured_output_agent
4. sessions_and_state
5. persistent_agent