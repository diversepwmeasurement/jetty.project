//
// ========================================================================
// Copyright (c) 1995 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.ee10.maven.plugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.jetty.ee10.quickstart.QuickStartConfiguration;
import org.eclipse.jetty.ee10.quickstart.QuickStartConfiguration.Mode;
import org.eclipse.jetty.ee10.servlet.ServletHandler;
import org.eclipse.jetty.ee10.webapp.Configurations;
import org.eclipse.jetty.maven.AbstractJettyEmbedder;
import org.eclipse.jetty.maven.ServerSupport;
import org.eclipse.jetty.server.handler.ContextHandler;

/**
 * JettyEmbedder
 * 
 * Starts jetty within the current process. 
 */
public class JettyEmbedder extends AbstractJettyEmbedder
{
    protected MavenWebAppContext webApp;

    public List<ContextHandler> getContextHandlers()
    {
        return contextHandlers;
    }
    
    public void setWebApp(MavenWebAppContext app)
    {
        webApp = app;
    }
    
    protected void redeployWebApp() throws Exception
    {
        stopWebApp();

        //clear the ServletHandler, which may have
        //remembered "durable" Servlets, Filters, Listeners
        //from the context xml file, but as we will re-apply
        //the context xml, we should not retain them
        webApp.setServletHandler(new ServletHandler());
        
        //regenerate config properties
        applyWebAppProperties();

        webApp.start();
    }

   @Override
    public void stopWebApp() throws Exception
    {
        if (webApp != null && !webApp.isStopped())
            webApp.stop();
    }

    /**
     * Configure the webapp
     * @throws Exception if there is an unspecified problem
     */
    public void configureWebApp() throws Exception
    {
        //Set up list of default Configurations to apply to a webapp
        Configurations.setServerDefault(server);

        /* Configure the webapp */
        if (webApp == null)
            webApp = new MavenWebAppContext();

        applyWebAppProperties();

        //If there is a quickstart file, then quickstart the webapp.
        if (webApp.getTempDirectory() != null)
        {
            Path qs = webApp.getTempDirectory().toPath().resolve("quickstart-web.xml");
            if (Files.exists(qs) && Files.isRegularFile(qs))
            {
                webApp.addConfiguration(new MavenQuickStartConfiguration());
                webApp.setAttribute(QuickStartConfiguration.QUICKSTART_WEB_XML, qs);
                webApp.setAttribute(QuickStartConfiguration.MODE, Mode.QUICKSTART);
            }
        }
    }

    public void applyWebAppProperties() throws Exception
    {
        super.applyWebAppProperties();
        WebAppPropertyConverter.fromProperties(webApp, webAppProperties, server, jettyProperties);
    }

    public void addWebAppToServer() throws Exception
    {
        //add the webapp to the server
        ServerSupport.addWebApplication(server, webApp);
    }
}
