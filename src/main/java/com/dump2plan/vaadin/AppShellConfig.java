package com.dump2plan.vaadin;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.theme.Theme;

@Push(value = PushMode.AUTOMATIC, transport = Transport.LONG_POLLING)
@Theme("dump2plan")
public class AppShellConfig implements AppShellConfigurator {
}
