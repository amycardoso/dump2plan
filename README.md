# dump2plan

Transform unstructured "brain dumps" into structured project plans using AI.

Built with [Embabel Agent Framework](https://docs.embabel.com/embabel-agent/guide/0.3.5-SNAPSHOT) (GOAP planning), [Vaadin](https://vaadin.com/) chat UI, and [Spring Boot](https://spring.io/projects/spring-boot).

## How It Works

1. Paste your brain dump into the chat
2. The AI agent analyzes your text and asks clarifying questions (HITL)
3. After gathering context, it generates a structured project plan with milestones, tasks, priorities, and dependencies
4. Export your plan as Markdown or JSON

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 24 |
| Framework | Spring Boot 3.5.10 |
| Agent Framework | Embabel Agent 0.3.5-SNAPSHOT |
| UI | Vaadin 24.6.4 |
| Default LLM | Anthropic Claude |

## Prerequisites

- Java 24+
- Maven 3.9+
- An LLM API key (`ANTHROPIC_API_KEY` or `OPENAI_API_KEY`)

## Quick Start

```bash
export ANTHROPIC_API_KEY=your-key-here
./mvnw spring-boot:run
```

Open http://localhost:8080 and log in with demo credentials:

| Username | Password   |
|----------|------------|
| `alice`  | `password` |
| `bob`    | `password` |

To use OpenAI instead, set `OPENAI_API_KEY` (the Maven profile auto-activates).

## GOAP Planning Chain

The agent decomposes plan generation into four GOAP actions:

```
UserInput -> [analyze] -> ExtractedIdeas -> [clarify/HITL] -> ClarifiedContext
  -> [structure] -> ProjectStructure -> [finalize] -> StructuredPlan
```

Three actor personas with different LLMs optimize cost and quality:

| Actor      | Role                               | Default LLM        |
|------------|------------------------------------|---------------------|
| `analyzer` | Fast idea extraction               | `claude-haiku-4-5`  |
| `planner`  | Plan structuring                   | `claude-sonnet-4-5` |
| `reviewer` | Validation and risk identification | `claude-sonnet-4-5` |

See [ARCHITECTURE.md](ARCHITECTURE.md) for the full system design, including HITL patterns, Blackboard types, and multi-LLM actor configuration.

## Project Structure

```
src/main/java/com/dump2plan/
  agent/              GOAP agent, chatbot actions, configuration
  model/              Domain records (ExtractedIdeas, StructuredPlan, etc.)
  service/            Plan export (Markdown, JSON)
  security/           Spring Security + Vaadin login
  user/               User model and service
  vaadin/             Chat UI, plan renderer, HITL prompts, export buttons
src/main/resources/
  prompts/            Jinja2 prompt templates (personas, objectives, elements)
  themes/dump2plan/   Vaadin Lumo theme overrides
  application.yml     Configuration (actors, LLMs, Embabel platform)
```

## Running Tests

```bash
# Unit tests (excludes *IT.java)
./mvnw test

# Integration tests (requires API key)
./mvnw verify -Dit.test=PlanGenerationIT
```

## Building for Production

```bash
./mvnw clean package -Pproduction
java -jar target/dump2plan-0.1.0-SNAPSHOT.jar
```

The `production` profile triggers Vaadin's optimized frontend build.

## Configuration

Key properties in `application.yml`:

| Property                        | Description               | Default              |
|---------------------------------|---------------------------|----------------------|
| `dump2plan.chat.llm`            | Chat response LLM         | `claude-sonnet-4-5`  |
| `dump2plan.actors.analyzer.llm` | Analysis LLM (fast/cheap) | `claude-haiku-4-5`   |
| `dump2plan.actors.planner.llm`  | Planning LLM (strong)     | `claude-sonnet-4-5`  |
| `dump2plan.actors.reviewer.llm` | Review LLM                | `claude-sonnet-4-5`  |
| `dump2plan.persona`             | Active persona template   | `planner`            |
| `dump2plan.objective`           | Active objective template  | `brain-dump-to-plan` |

## License

MIT
