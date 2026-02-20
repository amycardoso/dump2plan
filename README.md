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
mvn spring-boot:run
```

Open http://localhost:8080 and log in with `alice` / `password`.

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) for the full system design, including GOAP planning flow, HITL patterns, and multi-LLM actor configuration.

## Project Structure

```
com.dump2plan/
  agent/          # Embabel GOAP agent + chat actions
  model/          # Domain records (Blackboard types)
  service/        # Export services
  vaadin/         # Chat UI components
  security/       # Spring Security + Vaadin
  user/           # User management
```

## License

MIT
