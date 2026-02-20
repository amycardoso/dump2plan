# dump2plan Architecture

> A Spring Boot application that transforms unstructured "brain dumps" into structured project plans, powered by the Embabel Agent Framework with GOAP planning, HITL clarifying questions, and a Vaadin chat UI.

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Architecture Layers](#architecture-layers)
3. [Data Model](#data-model)
4. [Embabel Agent Design](#embabel-agent-design)
5. [Vaadin UI Design](#vaadin-ui-design)
6. [Package Structure](#package-structure)
7. [Build Configuration](#build-configuration)
8. [Configuration Properties](#configuration-properties)
9. [Implementation Roadmap](#implementation-roadmap)
10. [Key Architectural Decisions](#key-architectural-decisions)

---

## 1. System Overview

### Purpose

dump2plan accepts freeform text ("brain dumps") from users via a conversational chat interface and applies agentic AI planning to produce structured project plans containing tasks, milestones, dependencies, and timelines. Unlike a simple LLM wrapper, it uses the Embabel Agent Framework's GOAP (Goal-Oriented Action Planning) algorithm to decompose the transformation into discrete, composable actions that can be reordered dynamically based on input characteristics.

The agent uses **Human-in-the-Loop (HITL)** to pause mid-execution and ask clarifying questions ("What's the timeline?", "How large is the team?") before generating the final plan. This hybrid approach combines GOAP's deterministic planning with conversational context gathering.

### High-Level Data Flow

```
User pastes brain dump in chat
        |
        v
  +------------------+
  |   Vaadin ChatView |  (urbot/stashbot pattern)
  +------------------+
        |
        v
  +------------------+
  |  Embabel Agent    |  (GOAP Planner, Blackboard)
  |  Platform         |  (@Agent, @Action, @AchievesGoal)
  +------------------+
        |
        v
  +------------------+
  |  HITL: confirm()  |  Agent pauses, asks clarifying questions
  |  / fromForm()     |  User responds in chat, execution resumes
  +------------------+
        |
        v
  +------------------+
  |  Actor Pattern    |  analyzer (fast) -> planner (strong) -> reviewer
  |  Multi-LLM        |  (Anthropic Claude default)
  +------------------+
        |
        v
  +------------------+
  |  StructuredPlan   |  (Tasks, Milestones, Dependencies)
  +------------------+
        |
        v
  +------------------+
  |  Vaadin ChatView  |  Plan rendered in chat + export options
  +------------------+
```

### Core Principles

- **E2E Type Safety**: All data transformations use strongly-typed Java records with Jackson annotations, validated at compile time.
- **Observability**: Spring Boot Actuator + Embabel agent observability module for monitoring agent execution, action tracing, and LLM call logging.
- **Testability**: Agent actions are pure functions with injected dependencies; each action can be unit tested with `FakeOperationContext` from `embabel-agent-test`.
- **Readability**: Clean separation between agent logic, domain model, UI, and configuration.

---

## 2. Architecture Layers

### Layer Diagram

```
+---------------------------------------------------------+
|                    Presentation Layer                     |
|  Vaadin 24 (Server-Side, Chat Pattern)                  |
|  - ChatView (conversational UI, main route)              |
|  - ChatMessageBubble (message rendering)                 |
|  - HITL form/confirm dialogs (inline in chat)            |
+---------------------------------------------------------+
|                    Application Layer                     |
|  Spring Boot 3.5.x Services                             |
|  - ChatActions (@EmbabelComponent)                       |
|  - PlanExportService (Markdown, JSON export)             |
+---------------------------------------------------------+
|                    Agent Layer                            |
|  Embabel Agent Framework                                 |
|  - BrainDumpPlannerAgent (GOAP-planned)                  |
|  - Actions: analyze, clarify (HITL), structure, finalize |
|  - Actors: analyzer, planner, reviewer (multi-LLM)       |
|  - Blackboard: UserInput -> ... -> StructuredPlan        |
+---------------------------------------------------------+
|                    Domain Layer                           |
|  Java Records                                            |
|  - ExtractedIdeas, ClarifiedContext, ProjectStructure    |
|  - StructuredPlan, Task, Milestone, Priority             |
+---------------------------------------------------------+
|                    Infrastructure Layer                   |
|  - LLM Providers (Anthropic default, OpenAI optional)    |
|  - In-memory storage (Phase 1)                           |
|  - Optional persistence (Phase 2+)                       |
+---------------------------------------------------------+
```

### Layer Responsibilities

| Layer | Responsibility | Key Dependencies |
|-------|---------------|-----------------|
| **Presentation** | Chat UI, message bubbles, HITL dialogs, plan display | Vaadin 24, Spring Security |
| **Application** | Chat orchestration, export, session management | Spring Boot 3.5.x |
| **Agent** | GOAP-planned transformation with HITL pauses | Embabel Agent Framework |
| **Domain** | Type-safe Blackboard records | Jackson, Bean Validation |
| **Infrastructure** | LLM provider abstraction | Anthropic/OpenAI via profiles |

---

## 3. Data Model

All domain types are Java records with Jackson annotations for type-safe LLM deserialization. They flow through the GOAP Blackboard as intermediate state.

### Input: UserInput (Built-in)

The Embabel framework provides `UserInput` as the built-in starting type for GOAP chains. It carries `content` (the brain dump text) and `timestamp`.

### Intermediate: ExtractedIdeas

```java
@JsonClassDescription("Ideas and action items extracted from a brain dump")
public record ExtractedIdeas(
    List<String> extractedTopics,
    List<String> extractedActions,
    List<String> extractedConstraints,
    String projectType,
    String estimatedComplexity,
    List<String> clarifyingQuestions
) implements PromptContributor {}
```

### Intermediate: ClarifiedContext

```java
@JsonClassDescription("User responses to clarifying questions, gathered via HITL")
public record ClarifiedContext(
    String timeline,
    String teamSize,
    String budgetConstraints,
    String additionalContext
) implements PromptContributor {}
```

### Intermediate: ProjectStructure

```java
@JsonClassDescription("Organized project structure with milestones and grouped tasks")
public record ProjectStructure(
    String title,
    String summary,
    List<Milestone> milestones,
    List<Task> tasks,
    String estimatedDuration
) {}
```

### Output: StructuredPlan

```java
@JsonClassDescription("Final structured project plan with priorities, dependencies, and validation")
public record StructuredPlan(
    @JsonPropertyDescription("Plan title derived from the brain dump")
    String title,

    @JsonPropertyDescription("High-level project summary")
    String summary,

    @JsonPropertyDescription("Ordered list of milestones")
    List<Milestone> milestones,

    @JsonPropertyDescription("All tasks with priorities and dependencies")
    List<Task> tasks,

    @JsonPropertyDescription("Overall estimated duration")
    String estimatedDuration,

    @JsonPropertyDescription("Identified risks and assumptions")
    List<String> risks,

    List<String> assumptions
) implements HasContent {}
```

### Supporting Types

```java
@JsonClassDescription("A milestone representing a significant checkpoint")
public record Milestone(
    String id,
    String name,
    String description,
    int orderIndex,
    List<String> taskIds
) {}

@JsonClassDescription("A concrete task within the project plan")
public record Task(
    String id,
    String title,
    String description,
    Priority priority,
    String milestoneId,
    List<String> dependsOn,
    String estimatedEffort,
    int orderIndex
) {}

public enum Priority {
    CRITICAL, HIGH, MEDIUM, LOW
}
```

### GOAP Auto-Chaining via Type Signatures

The GOAP planner uses **A* search over action type signatures** to automatically chain actions. Each action declares its input types (preconditions) and return type (postcondition). The **Blackboard** stores domain objects by type, and the planner discovers valid action sequences without explicit wiring:

```
UserInput (on Blackboard)
   |-- [analyzeInput: UserInput -> ExtractedIdeas]              (analyzer actor)
   |-- [gatherContext: ExtractedIdeas -> ClarifiedContext]      (HITL: confirm/fromForm)
   |-- [structurePlan: ExtractedIdeas, ClarifiedContext -> ProjectStructure]  (planner actor)
   |-- [finalizePlan: ProjectStructure, ExtractedIdeas -> StructuredPlan]     (reviewer actor) @AchievesGoal
```

Because `structurePlan` requires both `ExtractedIdeas` and `ClarifiedContext`, the HITL step must complete before planning begins. The GOAP planner discovers this dependency automatically from type signatures.

---

## 4. Embabel Agent Design

### Agent: BrainDumpPlannerAgent

The core agent uses GOAP planning with **HITL pauses** and **multi-LLM actors**. This follows the tripper pattern: a linear GOAP chain where each action produces a typed output that flows through the Blackboard.

```java
@Agent(description = "Transforms unstructured brain dumps into structured project plans")
public class BrainDumpPlannerAgent {

    private final Dump2PlanProperties properties;

    @Action(cost = 0.3)
    public ExtractedIdeas analyzeInput(UserInput input, OperationContext ctx) {
        return properties.actors().analyzer().promptRunner(ctx)
            .createObject(
                "Analyze this brain dump. Extract topics, action items, constraints, " +
                "project type, complexity, and generate clarifying questions: " +
                input.getContent(),
                ExtractedIdeas.class
            );
    }

    @Action(cost = 0.1)
    public ClarifiedContext gatherContext(
            ExtractedIdeas ideas,
            OperationContext ctx) {
        // HITL: Pause execution and ask the user clarifying questions
        return ctx.confirm(
            ideas,
            "Before I create your plan, I have a few questions:\n" +
            String.join("\n", ideas.clarifyingQuestions())
        );
        // Alternative: structured form input
        // return ctx.fromForm("Project Details", ClarifiedContext.class);
    }

    @Action(cost = 0.8)
    public ProjectStructure structurePlan(
            ExtractedIdeas ideas,
            ClarifiedContext context,
            OperationContext ctx) {
        return properties.actors().planner().promptRunner(ctx)
            .createObject(
                "Create a structured project plan with milestones and tasks. " +
                "Ideas: " + ideas + " Context: " + context,
                ProjectStructure.class
            );
    }

    @AchievesGoal(description = "A validated, prioritized, structured project plan")
    @Action(cost = 0.7)
    public StructuredPlan finalizePlan(
            ProjectStructure structure,
            ExtractedIdeas ideas,
            OperationContext ctx) {
        return properties.actors().reviewer().promptRunner(ctx)
            .createObject(
                "Finalize this project plan: validate completeness, prioritize tasks, " +
                "assign dependencies, estimate effort, identify risks and assumptions. " +
                "Structure: " + structure + " Original ideas: " + ideas,
                StructuredPlan.class
            );
    }
}
```

### Human-in-the-Loop (HITL) Pattern

The HITL pattern from tripper enables the agent to **pause mid-GOAP-execution** to gather user input:

- **`ctx.confirm(payload, message)`**: Pauses the agent process (state = `WAITING`), displays the message to the user in the chat UI, waits for a response, then resumes execution with the user's answer.
- **`ctx.fromForm(title, dataClass)`** / **`WaitFor.formSubmission()`**: Pauses and collects structured input via a form rendered in the UI.

The framework handles the pause/resume lifecycle automatically. The Vaadin ChatView renders the HITL prompt as a chat message with input fields, and when the user responds, the agent process resumes from where it left off.

This replaces the need for a separate "conversational agent" -- the GOAP agent itself can ask clarifying questions at any point in its execution pipeline.

### Actor Pattern (Multi-LLM Personas)

Following the tripper pattern, actors allow different LLM configurations for different actions:

```yaml
# application.yml
dump2plan:
  actors:
    analyzer:
      persona: "analyzer"
      llm: "claude-haiku-4-5"      # Fast, cheap for initial analysis
    planner:
      persona: "planner"
      llm: "claude-sonnet-4-5"     # Strong reasoning for plan structure
    reviewer:
      persona: "reviewer"
      llm: "claude-sonnet-4-5"     # Analytical for validation
```

Each actor is configured with a persona (Jinja2 template) and an LLM model. In actions, use `properties.actors().analyzer().promptRunner(ctx)` to get the actor-specific prompt runner.

### ChatActions (Chat Integration)

The `@EmbabelComponent` handles chat messages and delegates to the GOAP agent:

```java
@EmbabelComponent
public class ChatActions {

    private final AgentPlatform agentPlatform;
    private final Dump2PlanProperties properties;

    @Action
    User bindUser(OperationContext context) {
        return context.getProcessContext().getProcessOptions()
            .getIdentities().getForUser();
    }

    @Action(canRerun = true, trigger = UserMessage.class)
    void respond(Conversation conversation, User user, ActionContext context) {
        var assistantMessage = context.ai()
            .withLlm(properties.chat().llm())
            .withReferences(references)
            .rendering("dump2plan")
            .respondWithSystemPrompt(conversation, Map.of(
                "properties", properties,
                "user", user
            ));
        context.sendMessage(conversation.addMessage(assistantMessage));
    }
}
```

### Agent Configuration

```java
@Configuration
@ConfigurationPropertiesScan
public class PlannerConfiguration {

    @Bean
    public Chatbot chatbot(AgentPlatform platform, Dump2PlanProperties properties) {
        return AgentProcessChatbot.utilityFromPlatform(
            platform,
            new InMemoryConversationFactory(),
            new Verbosity()
                .withShowPrompts(properties.chat().showPrompts())
                .withShowLlmResponses(properties.chat().showResponses())
        );
    }
}
```

### GOAP Planning Flow

1. **Initial State**: `UserInput` (brain dump text) placed on the Blackboard.
2. **Action 1 -- Analyze** (`analyzer` actor, fast model): Extract ideas, topics, constraints, and generate clarifying questions. Places `ExtractedIdeas` on the Blackboard.
3. **Action 2 -- Clarify** (HITL `confirm()`): Agent pauses, asks user clarifying questions in chat. User responds. Places `ClarifiedContext` on the Blackboard.
4. **Action 3 -- Structure** (`planner` actor, strong model): Build the plan structure with milestones and tasks using both ideas and context. Places `ProjectStructure` on the Blackboard.
5. **Action 4 -- Finalize** (`reviewer` actor, `@AchievesGoal`): Validate, prioritize, assign dependencies, estimate effort. Places `StructuredPlan` on the Blackboard. **Goal achieved.**

### Concurrent Task Elaboration

For complex plans with many tasks, use `context.parallelMap()` (from tripper) to elaborate tasks concurrently:

```java
@Action
public ElaboratedTasks elaborateTasks(ProjectStructure structure, OperationContext ctx) {
    var elaborated = ctx.parallelMap(
        structure.tasks(),
        task -> ctx.ai().createObject(
            "Add implementation details and subtasks for: " + task,
            ElaboratedTask.class
        ),
        /* maxConcurrency */ 6
    );
    return new ElaboratedTasks(elaborated);
}
```

### Testing Agent Actions

```java
// Unit test with FakeOperationContext
@Test
void analyzeInput_extractsTopicsFromBrainDump() {
    var fakeCtx = new FakeOperationContext();
    fakeCtx.getPromptRunner().setObjectToReturn(expectedIdeas);

    var agent = new BrainDumpPlannerAgent(properties);
    var result = agent.analyzeInput(testInput, fakeCtx);

    assertThat(result.extractedTopics()).containsExactly("auth", "dashboard", "api");
}
```

Integration tests use `embabel-agent-eval` with `TranscriptScorer` for LLM-as-judge evaluation:

```java
@SpringBootTest(classes = TestDump2PlanApplication.class, webEnvironment = NONE)
@ActiveProfiles("it")
class PlanGenerationIT {
    // Full LLM-as-judge evaluation pipeline
}
```

### Prompt Templates (Jinja2)

Following the Embabel composable template pattern:

```
src/main/resources/prompts/
  dump2plan.jinja                      # Main entry point
  elements/
    guardrails.jinja                   # Safety constraints
    personalization.jinja              # Dynamic persona/objective loader
    user.jinja                         # Current user context
  personas/
    analyzer.jinja                     # "You are a rapid idea extractor..."
    planner.jinja                      # "You are an expert project planner..."
    reviewer.jinja                     # "You are an analytical plan reviewer..."
  objectives/
    brain-dump-to-plan.jinja           # Planning objective
```

**personalization.jinja** dynamically includes the appropriate persona and objective templates based on config properties, enabling easy swapping without code changes.

---

## 5. Vaadin UI Design

### Chat-Based Interface

The UI follows the **urbot/stashbot ChatView pattern** -- a conversational interface where the user interacts with the agent through messages. This enables natural HITL interactions: the agent asks clarifying questions inline in the chat, and the user responds conversationally.

#### AppShellConfig

```java
@Push(value = PushMode.AUTOMATIC, transport = Transport.LONG_POLLING)
@Theme("dump2plan")
public class AppShellConfig implements AppShellConfigurator {
}
```

#### Route Structure

| Route | View Class | Purpose |
|-------|-----------|---------|
| `/` | `ChatView` | Main conversational interface |
| `/login` | `LoginView` | Authentication |

#### ChatView Layout

```
+--------------------------------------------------+
|  dump2plan                              [Export] [?]|
+--------------------------------------------------+
|                                                    |
|  +----------------------------------------------+ |
|  |  Messages Scroller                            | |
|  |                                                | |
|  |  [User]  Here's my brain dump: I need to      | |
|  |          build a mobile app for tracking       | |
|  |          fitness goals. It should have...      | |
|  |                                                | |
|  |  [Agent] I've analyzed your brain dump and     | |
|  |          extracted 8 action items across 3     | |
|  |          categories. Before I create the plan: | |
|  |                                                | |
|  |          - What's your target timeline?        | |
|  |          - How large is the development team?  | |
|  |          - Any budget constraints?             | |
|  |                                                | |
|  |  [User]  3 months, 2 developers, bootstrapped | |
|  |                                                | |
|  |  [Agent] Processing... (Structuring plan...)   | |
|  |                                                | |
|  |  [Agent] Here's your project plan:             | |
|  |                                                | |
|  |  # Fitness Tracker App                         | |
|  |  **Summary**: A mobile fitness tracking app... | |
|  |  **Duration**: 12 weeks                        | |
|  |                                                | |
|  |  > MILESTONE 1: Foundation (Weeks 1-3)         | |
|  |    - Set up project repository      [HIGH]     | |
|  |    - Define tech stack              [HIGH]     | |
|  |    - Create wireframes              [MEDIUM]   | |
|  |                                                | |
|  |  > MILESTONE 2: Core Features (Weeks 4-8)     | |
|  |    - Implement auth flow            [CRITICAL] | |
|  |    - Build goal tracking UI         [HIGH]     | |
|  |    ...                                         | |
|  |                                                | |
|  |  [Export as Markdown] [Export as JSON]          | |
|  |                                                | |
|  +----------------------------------------------+ |
|                                                    |
|  +----------------------------------------------+ |
|  |  [TextArea: Type your message...]   [Send]    | |
|  +----------------------------------------------+ |
|                                                    |
+--------------------------------------------------+
```

### Key UI Components

| Component | Vaadin Base | Purpose |
|-----------|-------------|---------|
| `AppShellConfig` | `AppShellConfigurator` | `@Push(AUTOMATIC, LONG_POLLING)` + `@Theme("dump2plan")` |
| `ChatView` | `VerticalLayout` | `@Route("")`, main chat interface (follows urbot pattern) |
| `ChatMessageBubble` | `Div` | Renders individual messages with markdown support |
| `HitlPrompt` | `VerticalLayout` | Renders HITL `confirm()`/`fromForm()` dialogs inline in chat |
| `PlanRenderer` | `VerticalLayout` | Renders `StructuredPlan` with `Details`/`Accordion` for milestones |
| `ExportButtons` | `HorizontalLayout` | Markdown and JSON export actions |
| `LoginView` | `LoginForm` | Spring Security login page |

### Real-Time Updates (Vaadin Push + OutputChannel)

The chat uses background threads with `UI.access()` for thread-safe push updates:

```java
@Route("")
@PermitAll
public class ChatView extends VerticalLayout {

    private final Chatbot chatbot;
    private final VerticalLayout messagesLayout;

    public ChatView(Chatbot chatbot) {
        this.chatbot = chatbot;
        // ... layout setup following urbot ChatView pattern
    }

    private void sendMessage(String text) {
        var ui = UI.getCurrent();
        addMessageBubble("user", text);

        new Thread(() -> {
            var session = getOrCreateSession();
            var response = chatbot.send(
                new UserMessage(text),
                session,
                new VaadinOutputChannel(ui, messagesLayout)
            );
            ui.access(() -> addMessageBubble("agent", response.content()));
        }).start();
    }
}
```

**VaadinOutputChannel** implements the Embabel `OutputChannel` interface to show real-time progress (tool calls, GOAP step indicators) as the agent processes:

```java
public class VaadinOutputChannel implements OutputChannel {
    private final UI ui;
    private final VerticalLayout messagesLayout;

    @Override
    public void onActionStarted(String actionName) {
        ui.access(() -> showProcessingIndicator(actionName));
    }

    @Override
    public void onActionCompleted(String actionName) {
        ui.access(() -> updateProcessingIndicator(actionName, true));
    }
}
```

### HITL Integration in Chat

When the agent calls `ctx.confirm()` or `ctx.fromForm()`, the framework puts the process in `WAITING` state. The ChatView detects this and renders the HITL prompt inline as a chat message with input fields. When the user responds, the agent process resumes automatically.

### Session State Management

Chat sessions and plan history are managed via `VaadinSession` and `InMemoryConversationFactory`:

```java
var session = VaadinSession.getCurrent()
    .getAttribute(ChatSession.class);
```

---

## 6. Package Structure

```
com.dump2plan/
|
+-- Dump2PlanApplication.java              # @SpringBootApplication entry point
+-- Dump2PlanProperties.java               # @ConfigurationProperties record
|
+-- agent/                                 # Embabel Agent Layer
|   +-- BrainDumpPlannerAgent.java         # @Agent with GOAP actions + HITL
|   +-- ChatActions.java                   # @EmbabelComponent for chat integration
|   +-- PlannerConfiguration.java          # Chatbot bean + @ConfigurationPropertiesScan
|
+-- model/                                 # Domain Model (Blackboard types)
|   +-- ExtractedIdeas.java                # Intermediate: parsed ideas + questions
|   +-- ClarifiedContext.java              # Intermediate: HITL user responses
|   +-- ProjectStructure.java              # Intermediate: organized structure
|   +-- StructuredPlan.java                # Output: final plan record
|   +-- Milestone.java                     # Milestone record
|   +-- Task.java                          # Task record
|   +-- Priority.java                      # Priority enum
|
+-- service/                               # Application Services
|   +-- PlanExportService.java             # Export to Markdown/JSON
|
+-- vaadin/                                # Vaadin UI Components
|   +-- AppShellConfig.java                # @Push(AUTOMATIC, LONG_POLLING) + @Theme
|   +-- ChatView.java                      # @Route(""), main chat interface
|   +-- ChatMessageBubble.java             # Message rendering with markdown
|   +-- HitlPrompt.java                    # HITL dialog rendering in chat
|   +-- PlanRenderer.java                  # StructuredPlan display with Accordion
|   +-- ExportButtons.java                 # Export action buttons
|   +-- VaadinOutputChannel.java           # OutputChannel for real-time progress
|
+-- security/                              # Security Configuration
|   +-- SecurityConfiguration.java         # extends VaadinWebSecurity
|   +-- LoginView.java                     # Login page
|
+-- user/                                  # User Management
|   +-- Dump2PlanUser.java                 # implements com.embabel.agent.api.identity.User
|   +-- Dump2PlanUserService.java          # UserService + UserDetailsService
|
src/main/resources/
+-- application.yml                        # Main configuration
+-- prompts/
|   +-- dump2plan.jinja                    # Main prompt entry point
|   +-- elements/
|   |   +-- guardrails.jinja               # Safety constraints
|   |   +-- personalization.jinja          # Dynamic persona/objective loader
|   |   +-- user.jinja                     # User context
|   +-- personas/
|   |   +-- analyzer.jinja                 # Fast analysis persona
|   |   +-- planner.jinja                  # Expert planner persona
|   |   +-- reviewer.jinja                 # Analytical reviewer persona
|   +-- objectives/
|       +-- brain-dump-to-plan.jinja       # Planning objective
+-- themes/
    +-- dump2plan/
        +-- styles.css                     # Custom Lumo theme

src/test/java/com/dump2plan/
+-- TestDump2PlanApplication.java          # Test boot class (excludes Vaadin/security)
+-- TestSecurityConfiguration.java         # Test-only user service bean
+-- TemplateRenderingTest.java             # Validates Jinja templates render
+-- agent/
|   +-- BrainDumpPlannerAgentTest.java     # Unit tests with FakeOperationContext
+-- PlanGenerationIT.java                  # Integration test with LLM-as-judge

src/test/resources/
+-- application-it.yml                     # Integration test config
```

---

## 7. Build Configuration

### pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.10</version>
        <relativePath/>
    </parent>

    <groupId>com.dump2plan</groupId>
    <artifactId>dump2plan</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <name>dump2plan</name>
    <description>Transform brain dumps into structured project plans</description>
    <packaging>jar</packaging>

    <properties>
        <java.version>24</java.version>
        <vaadin.version>24.6.4</vaadin.version>
        <embabel-agent.version>0.3.5-SNAPSHOT</embabel-agent.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.vaadin</groupId>
                <artifactId>vaadin-bom</artifactId>
                <version>${vaadin.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Embabel Agent Framework -->
        <dependency>
            <groupId>com.embabel.agent</groupId>
            <artifactId>embabel-agent-starter</artifactId>
            <version>${embabel-agent.version}</version>
        </dependency>

        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Vaadin (version managed by BOM) -->
        <dependency>
            <groupId>com.vaadin</groupId>
            <artifactId>vaadin-spring-boot-starter</artifactId>
        </dependency>

        <!-- Markdown rendering -->
        <dependency>
            <groupId>org.commonmark</groupId>
            <artifactId>commonmark</artifactId>
            <version>0.24.0</version>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.embabel.agent</groupId>
            <artifactId>embabel-agent-test</artifactId>
            <version>${embabel-agent.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.embabel.agent</groupId>
            <artifactId>embabel-agent-eval</artifactId>
            <version>${embabel-agent.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <jvmArguments>--enable-native-access=ALL-UNNAMED</jvmArguments>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>**/*IT.java</exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <!-- LLM provider profiles (auto-activated by env var) -->
    <profiles>
        <profile>
            <id>openai-models</id>
            <activation>
                <property>
                    <name>env.OPENAI_API_KEY</name>
                </property>
            </activation>
            <dependencies>
                <dependency>
                    <groupId>com.embabel.agent</groupId>
                    <artifactId>embabel-agent-starter-openai</artifactId>
                    <version>${embabel-agent.version}</version>
                </dependency>
            </dependencies>
        </profile>
        <profile>
            <id>anthropic-models</id>
            <activation>
                <property>
                    <name>env.ANTHROPIC_API_KEY</name>
                </property>
            </activation>
            <dependencies>
                <dependency>
                    <groupId>com.embabel.agent</groupId>
                    <artifactId>embabel-agent-starter-anthropic</artifactId>
                    <version>${embabel-agent.version}</version>
                </dependency>
            </dependencies>
        </profile>
        <profile>
            <id>production</id>
            <dependencies>
                <dependency>
                    <groupId>com.vaadin</groupId>
                    <artifactId>vaadin</artifactId>
                    <exclusions>
                        <exclusion>
                            <groupId>com.vaadin</groupId>
                            <artifactId>vaadin-dev</artifactId>
                        </exclusion>
                    </exclusions>
                </dependency>
            </dependencies>
        </profile>
    </profiles>

    <repositories>
        <repository>
            <id>embabel-releases</id>
            <url>https://repo.embabel.com/artifactory/libs-release</url>
        </repository>
        <repository>
            <id>embabel-snapshots</id>
            <url>https://repo.embabel.com/artifactory/libs-snapshot</url>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </repository>
    </repositories>
</project>
```

### Key Build Notes

- **Java 24** -- Embabel JARs compiled for Java 21 run on the Java 24 JVM via forward compatibility. Spring Boot 3.5.x has best-effort Java 24 support. Spring Boot 4.x is NOT supported by Embabel.
- **No RAG/vector store dependencies** -- dump2plan does not need document retrieval; it transforms text input directly.
- **Anthropic as primary LLM** -- activated via `ANTHROPIC_API_KEY` env var. OpenAI available as alternative profile.
- **Vaadin 24.6.4** with BOM for version management and `LONG_POLLING` transport.
- **Production build**: `mvn clean package -Pproduction` for optimized frontend bundle.
- **Test separation**: `*IT.java` integration tests excluded from `mvn test`; run via `mvn verify`.

---

## 8. Configuration Properties

### application.yml

```yaml
server:
  port: 8080

spring:
  application:
    name: dump2plan

dump2plan:
  # Chat configuration
  chat:
    llm: "claude-sonnet-4-5"
    show-prompts: false
    show-responses: false

  # Actor pattern: different LLM personas for different GOAP actions
  actors:
    analyzer:
      persona: "analyzer"
      llm: "claude-haiku-4-5"         # Fast, cheap for initial extraction
    planner:
      persona: "planner"
      llm: "claude-sonnet-4-5"        # Strong reasoning for plan structure
    reviewer:
      persona: "reviewer"
      llm: "claude-sonnet-4-5"        # Analytical for validation

  # Persona and objective templates
  persona: "planner"
  objective: "brain-dump-to-plan"

# Embabel platform settings
embabel:
  agent:
    platform:
      http-client:
        read-timeout: 10m

# Vaadin configuration
vaadin:
  launch-browser: true
  pnpm:
    enable: true

# Actuator for observability
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
```

### Dump2PlanProperties.java

```java
@ConfigurationProperties(prefix = "dump2plan")
public record Dump2PlanProperties(
    @NestedConfigurationProperty ChatConfig chat,
    @NestedConfigurationProperty ActorsConfig actors,
    String persona,
    String objective
) {
    public record ChatConfig(
        String llm,
        boolean showPrompts,
        boolean showResponses
    ) {}

    public record ActorsConfig(
        ActorConfig analyzer,
        ActorConfig planner,
        ActorConfig reviewer
    ) {}

    public record ActorConfig(
        String persona,
        String llm
    ) {
        public PromptRunner promptRunner(OperationContext ctx) {
            return ctx.ai().withLlm(llm);
        }
    }
}
```

---

## 9. Implementation Roadmap

### Phase 1: Core MVP (Foundation)

**Goal**: Working chat interface that accepts a brain dump, asks clarifying questions via HITL, and produces a structured plan.

| Step | Component | Description |
|------|-----------|-------------|
| 1.1 | Project scaffold | Maven project with Java 24, Spring Boot 3.5.10, Embabel, Vaadin |
| 1.2 | Domain model | ExtractedIdeas, ClarifiedContext, ProjectStructure, StructuredPlan, Task, Milestone records |
| 1.3 | Agent setup | BrainDumpPlannerAgent with GOAP actions: analyze, clarify (HITL), structure, finalize |
| 1.4 | ChatActions | `@EmbabelComponent` with `respond` action + `bindUser` |
| 1.5 | Prompt templates | Composable Jinja2: personas (analyzer, planner, reviewer), objectives, elements |
| 1.6 | ChatView | Chat interface following urbot pattern with message bubbles and HITL rendering |
| 1.7 | Actor config | Multi-LLM actor setup in application.yml (analyzer, planner, reviewer) |
| 1.8 | Integration test | End-to-end test: brain dump in, plan out, with FakeOperationContext unit tests |

### Phase 2: Polish & Export

**Goal**: Production-quality UI with export capabilities and session management.

| Step | Component | Description |
|------|-----------|-------------|
| 2.1 | VaadinOutputChannel | Real-time GOAP step progress in chat via OutputChannel |
| 2.2 | PlanRenderer | Rich plan display with Details/Accordion milestones in chat |
| 2.3 | Export service | Markdown and JSON export of StructuredPlan |
| 2.4 | Error handling | Graceful handling of LLM failures, input validation, timeout recovery |
| 2.5 | Theming | Custom Lumo theme in `src/main/resources/themes/dump2plan/` |
| 2.6 | Security | VaadinWebSecurity with login, demo users, @PermitAll on ChatView |

### Phase 3: Enhancement

**Goal**: Advanced features for power users.

| Step | Component | Description |
|------|-----------|-------------|
| 3.1 | Iterative refinement | "Refine this plan" via follow-up chat messages |
| 3.2 | Concurrent elaboration | `parallelMap()` for parallel task elaboration in complex plans |
| 3.3 | Multiple output formats | Gantt-style timeline, Kanban board views, Asana export |
| 3.4 | Persistent storage | Database-backed plan storage (H2 or PostgreSQL) |
| 3.5 | Multi-model support | Allow users to select LLM provider/model per session |

---

## 10. Key Architectural Decisions

### ADR-1: Embabel Agent Framework with GOAP + HITL

**Decision**: Use Embabel Agent Framework with GOAP planning and HITL for clarifying questions, instead of direct LLM calls or a pure chatbot pattern.

**Rationale**:
- GOAP enables dynamic action sequencing via A* search over type signatures -- no manual orchestration.
- HITL (`confirm()`, `fromForm()`) allows the agent to pause mid-execution and gather context from the user, producing better plans.
- Type-safe domain model integration via `OperationContext.ai().createObject()` eliminates manual JSON parsing.
- The Blackboard pattern auto-chains actions based on input/output types.
- Actions are independently testable via `FakeOperationContext`.
- Actor pattern enables multi-LLM persona optimization (fast model for analysis, strong model for planning).

**Trade-off**: Adds dependency on a less mature framework, but gains HITL, GOAP planning, and multi-LLM orchestration.

### ADR-2: Chat-Based UI with Vaadin (urbot/stashbot pattern)

**Decision**: Use a conversational chat interface (following urbot/stashbot `ChatView` pattern) instead of a form-based input/output UI.

**Rationale**:
- HITL `confirm()`/`fromForm()` dialogs render naturally as chat messages with inline input.
- Users can iteratively refine plans via follow-up messages.
- Consistent with reference applications (stashbot, urbot) in the Embabel ecosystem.
- `@Push(AUTOMATIC, LONG_POLLING)` enables real-time GOAP step progress.
- Simpler deployment: single JAR artifact (`mvn clean package -Pproduction`).

**Trade-off**: Chat UI is less structured than a dedicated form, but HITL provides the structured data gathering when needed.

### ADR-3: Actor Pattern for Multi-LLM Optimization

**Decision**: Use the tripper actor pattern to assign different LLM models to different GOAP actions.

**Rationale**:
- **Analyzer** (fast, cheap model like Claude Haiku): Quick initial extraction of ideas and clarifying questions.
- **Planner** (strong model like Claude Sonnet): High-quality plan structuring requiring complex reasoning.
- **Reviewer** (analytical model): Validation and consistency checking.
- Optimizes cost: fast models for simple tasks, strong models only where needed.
- Each actor has its own Jinja2 persona template for specialized system prompts.

**Trade-off**: More complex configuration, but significant cost savings and quality optimization.

### ADR-4: Java 24 on Spring Boot 3.5.x

**Decision**: Use Java 24 (latest) with Spring Boot 3.5.10, not Spring Boot 4.x.

**Rationale**:
- Java 24 provides latest language features and performance improvements.
- Embabel JARs are compiled for Java 21 and run on Java 24 via forward compatibility.
- Spring Boot 3.5.x has best-effort Java 24 support.
- Spring Boot 4.x is NOT yet supported by Embabel Agent Framework.

**Trade-off**: Best-effort (not fully certified) Java 24 support from Spring Boot, but no known issues.

### ADR-5: No RAG/Vector Store

**Decision**: Omit RAG infrastructure (Lucene, Neo4j, Tika) that exists in stashbot/urbot.

**Rationale**:
- dump2plan transforms user-provided text via HITL conversation, not uploaded documents.
- No semantic search or document retrieval is needed for the core use case.
- Reduces complexity, dependencies, and infrastructure requirements.

**Trade-off**: If future features require document-grounded planning (e.g., "plan based on this PRD"), RAG can be added later.

### ADR-6: Anthropic Claude as Default LLM

**Decision**: Use Anthropic Claude models as the default LLM provider.

**Rationale**:
- User preference for Anthropic.
- Claude Sonnet offers strong reasoning for plan generation.
- Claude Haiku provides fast, cost-effective analysis.
- Actor pattern allows mixing models per action.
- OpenAI remains available as an alternative via Maven profile.

**Trade-off**: Requires `ANTHROPIC_API_KEY` environment variable.

### ADR-7: Java Records for Domain Model

**Decision**: Use Java records for all domain types on the Blackboard.

**Rationale**:
- Immutable by default, aligning with functional data flow through GOAP actions.
- Built-in `equals()`, `hashCode()`, `toString()` reduce boilerplate.
- Jackson serialization works seamlessly with records.
- `PromptContributor` and `HasContent` interfaces add framework integration.

**Trade-off**: Cannot extend records (no inheritance). Acceptable for value objects.

### ADR-8: Jinja2 Composable Prompt Templates

**Decision**: Externalize all LLM prompts as composable Jinja2 template files with `elements/`, `personas/`, and `objectives/` directories.

**Rationale**:
- `personalization.jinja` dynamically includes persona/objective based on config properties.
- Prompts can be iterated on without recompiling.
- `guardrails.jinja` provides consistent safety constraints across all actors.
- Consistent with stashbot/urbot/tripper patterns.

**Trade-off**: Adds template complexity, but enables easy per-actor persona customization.

---

## Appendix A: Reference Application Comparison

| Aspect | stashbot | urbot | tripper | dump2plan |
|--------|----------|-------|---------|-----------|
| **Purpose** | RAG chatbot | RAG chatbot + memory | Travel planner | Brain dump to plan |
| **Language** | Java | Java | Kotlin | Java |
| **Agent Pattern** | Utility AI chatbot | Utility AI chatbot | GOAP agent | GOAP agent + HITL |
| **Vector Store** | Lucene | Neo4j | None | None |
| **Document Upload** | Yes (Tika) | Yes (Tika) | No | No |
| **HITL** | No | No | confirm() | confirm() / fromForm() |
| **Actor Pattern** | No | No | Yes (multi-LLM) | Yes (multi-LLM) |
| **UI** | Vaadin ChatView | Vaadin ChatView | htmx | Vaadin ChatView |
| **Primary Interaction** | Conversational | Conversational | Form + agent | Chat + HITL |
| **parallelMap()** | No | No | Yes | Yes (Phase 3) |
| **Status** | Archived | Active template | Active example | New project |

## Appendix B: Key References

| Resource | URL |
|----------|-----|
| Embabel Agent Docs | https://docs.embabel.com/embabel-agent/guide/0.3.5-SNAPSHOT |
| urbot (chat pattern reference) | https://github.com/embabel/urbot |
| tripper (GOAP + HITL reference) | https://github.com/embabel/tripper |
| stashbot (archived, simpler) | https://github.com/embabel/stashbot |
| Embabel Agent Framework | https://github.com/embabel/embabel-agent |

## Appendix C: Technology Stack Summary

| Category | Technology | Version |
|----------|-----------|---------|
| Language | Java | 24 |
| Framework | Spring Boot | 3.5.10 |
| Agent Framework | Embabel Agent | 0.3.5-SNAPSHOT |
| UI Framework | Vaadin | 24.6.4 |
| Build Tool | Maven | 3.9+ |
| Default LLM | Anthropic Claude | Sonnet 4.5 / Haiku 4.5 |
| Alternative LLM | OpenAI | Via profile |
| Markdown | CommonMark | 0.24.0 |
| Testing | JUnit 5, Embabel Agent Test/Eval | Managed |
| Monitoring | Spring Boot Actuator | Managed |
