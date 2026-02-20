package com.dump2plan.vaadin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.StreamResource;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class ExportButtons extends HorizontalLayout {

    public ExportButtons(String markdownContent) {
        setSpacing(true);
        addClassName("export-buttons");

        var copyButton = new Button("Copy Markdown", e -> {
            e.getSource().getElement().executeJs(
                "navigator.clipboard.writeText($0).then(() => {})",
                markdownContent
            );
            Notification.show("Copied to clipboard!",
                    2000, Notification.Position.BOTTOM_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        copyButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

        var resource = new StreamResource("plan.md",
            () -> new ByteArrayInputStream(markdownContent.getBytes(StandardCharsets.UTF_8)));
        resource.setContentType("text/markdown");

        var downloadAnchor = new Anchor(resource, "");
        downloadAnchor.getElement().setAttribute("download", true);
        var downloadButton = new Button("Download Markdown");
        downloadButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        downloadAnchor.add(downloadButton);

        add(copyButton, downloadAnchor);
    }
}
