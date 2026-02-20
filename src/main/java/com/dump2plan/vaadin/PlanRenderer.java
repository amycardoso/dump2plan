package com.dump2plan.vaadin;

import com.dump2plan.model.Milestone;
import com.dump2plan.model.StructuredPlan;
import com.dump2plan.model.Task;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;

public class PlanRenderer extends VerticalLayout {

    public PlanRenderer(StructuredPlan plan) {
        setPadding(false);
        setSpacing(true);
        addClassName("plan-renderer");

        add(new H3(plan.title()));
        add(new Paragraph(plan.summary()));
        add(new Span("Estimated duration: " + plan.estimatedDuration()));

        for (Milestone milestone : plan.milestones()) {
            List<Task> milestoneTasks = plan.tasks().stream()
                .filter(t -> milestone.id().equals(t.milestoneId()))
                .sorted((a, b) -> Integer.compare(a.orderIndex(), b.orderIndex()))
                .toList();

            var tasksLayout = new VerticalLayout();
            tasksLayout.setPadding(false);
            tasksLayout.setSpacing(false);

            for (Task task : milestoneTasks) {
                var taskDiv = new Div();
                taskDiv.addClassName("plan-task");

                var priorityBadge = new Span("[" + task.priority() + "]");
                priorityBadge.addClassName("priority-badge");
                priorityBadge.addClassName("priority-" + task.priority().name().toLowerCase());

                var taskTitle = new Span(task.title());
                taskTitle.addClassName("plan-task-title");

                taskDiv.add(taskTitle, priorityBadge);

                if (task.estimatedEffort() != null) {
                    var effort = new Span(" - " + task.estimatedEffort());
                    effort.addClassName("plan-task-effort");
                    taskDiv.add(effort);
                }

                tasksLayout.add(taskDiv);
            }

            var details = new Details(
                milestone.name() + " (" + milestoneTasks.size() + " tasks)",
                tasksLayout
            );
            details.setOpened(true);
            details.addClassName("plan-milestone");
            add(details);
        }

        if (plan.risks() != null && !plan.risks().isEmpty()) {
            var risksLayout = new VerticalLayout();
            risksLayout.setPadding(false);
            for (String risk : plan.risks()) {
                risksLayout.add(new Span("- " + risk));
            }
            add(new Details("Risks", risksLayout));
        }

        if (plan.assumptions() != null && !plan.assumptions().isEmpty()) {
            var assumptionsLayout = new VerticalLayout();
            assumptionsLayout.setPadding(false);
            for (String assumption : plan.assumptions()) {
                assumptionsLayout.add(new Span("- " + assumption));
            }
            add(new Details("Assumptions", assumptionsLayout));
        }
    }
}
