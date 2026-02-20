package com.dump2plan.vaadin;

import com.dump2plan.model.StructuredPlan;
import com.dump2plan.service.PlanExportService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.StreamResource;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class ExportButtons extends HorizontalLayout {

    public ExportButtons(StructuredPlan plan, PlanExportService exportService) {
        setSpacing(true);
        addClassName("export-buttons");

        var markdownButton = new Button("Export Markdown", e -> {
            try {
                String markdown = exportService.exportToMarkdown(plan);
                Notification.show("Markdown exported! Copy from console.",
                    3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Export failed: " + ex.getMessage(),
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        markdownButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        var jsonButton = new Button("Export JSON", e -> {
            try {
                String json = exportService.exportToJson(plan);
                Notification.show("JSON exported! Copy from console.",
                    3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Export failed: " + ex.getMessage(),
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        jsonButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        add(markdownButton, jsonButton);
    }
}
