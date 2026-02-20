package com.dump2plan.service;

import com.dump2plan.model.Milestone;
import com.dump2plan.model.StructuredPlan;
import com.dump2plan.model.Task;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlanExportService {

    private final ObjectMapper objectMapper;

    public PlanExportService() {
        this.objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public String exportToMarkdown(StructuredPlan plan) {
        var sb = new StringBuilder();
        sb.append("# ").append(plan.title()).append("\n\n");
        sb.append("**Summary**: ").append(plan.summary()).append("\n\n");
        sb.append("**Estimated Duration**: ").append(plan.estimatedDuration()).append("\n\n");

        for (Milestone milestone : plan.milestones()) {
            sb.append("## ").append(milestone.name()).append("\n\n");
            sb.append(milestone.description()).append("\n\n");

            List<Task> milestoneTasks = plan.tasks().stream()
                .filter(t -> milestone.id().equals(t.milestoneId()))
                .sorted((a, b) -> Integer.compare(a.orderIndex(), b.orderIndex()))
                .toList();

            for (Task task : milestoneTasks) {
                sb.append("- [ ] **").append(task.title()).append("** [")
                    .append(task.priority()).append("]\n");
                if (task.description() != null && !task.description().isBlank()) {
                    sb.append("  ").append(task.description()).append("\n");
                }
                if (task.estimatedEffort() != null) {
                    sb.append("  _Effort: ").append(task.estimatedEffort()).append("_\n");
                }
            }
            sb.append("\n");
        }

        if (plan.risks() != null && !plan.risks().isEmpty()) {
            sb.append("## Risks\n\n");
            for (String risk : plan.risks()) {
                sb.append("- ").append(risk).append("\n");
            }
            sb.append("\n");
        }

        if (plan.assumptions() != null && !plan.assumptions().isEmpty()) {
            sb.append("## Assumptions\n\n");
            for (String assumption : plan.assumptions()) {
                sb.append("- ").append(assumption).append("\n");
            }
        }

        return sb.toString();
    }

    public String exportToJson(StructuredPlan plan) {
        try {
            return objectMapper.writeValueAsString(plan);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export plan to JSON", e);
        }
    }
}
