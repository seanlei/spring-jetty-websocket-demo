package com.seanlei.demo.websocket.webapp;

import com.seanlei.demo.websocket.config.AppConfig;
import com.seanlei.demo.websocket.config.WebsocketConfig;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;
import org.eclipse.jetty.annotations.ServletContainerInitializersStarter;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.jsp.JettyJspServlet;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

/**
 * Created by Sean Lei on 6/20/16.
 */
@Component
public class App {
    @Value("${server.port}")
    private int serverPort;

    public void start(WebApplicationContext webCxt) {
        WebAppContext contextHandler = new WebAppContext();

        contextHandler.setContextPath("/");

        DispatcherServlet dispatcherServlet = new DispatcherServlet(webCxt);
        Server server = new Server(serverPort);

        String baseStr = "/webapp";
        URL baseUrl = App.class.getResource(baseStr);
        String basePath = baseUrl.toExternalForm();

        contextHandler.addServlet(new ServletHolder(dispatcherServlet), "/");
        contextHandler.setEventListeners(new EventListener[]{new ContextLoaderListener(webCxt)});
        contextHandler.setResourceBase(basePath);

        //support jsp
        Configuration.ClassList classlist = Configuration.ClassList
                .setServerDefault(server);
        classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
                "org.eclipse.jetty.annotations.AnnotationConfiguration");
        contextHandler.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*/[^/]*servlet-api-[^/]*\\.jar$|.*/javax.servlet.jsp.jstl-.*\\.jar$|.*/[^/]*taglibs.*\\.jar$");

        server.setHandler(contextHandler);
        try {
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        AnnotationConfigWebApplicationContext webCxt = new AnnotationConfigWebApplicationContext();
        webCxt.register(WebsocketConfig.class);
        webCxt.setParent(context);

        App app = context.getBean(App.class);
        try {
            app.startA(webCxt);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startA(WebApplicationContext webCxt) throws Exception {
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(serverPort);

        server.addConnector(connector);

        URI baseUri = getWebRootResourceUri();

        // Set JSP to use Standard JavaC always
        System.setProperty("org.apache.jasper.compiler.disablejsr199", "false");

        WebAppContext webAppContext = getWebAppContext(baseUri, getScratchDir());

        DispatcherServlet dispatcherServlet = new DispatcherServlet(webCxt);
        webAppContext.addServlet(new ServletHolder(dispatcherServlet), "/");
        webAppContext.setEventListeners(new EventListener[]{new ContextLoaderListener(webCxt)});
        server.setHandler(webAppContext);

        // Start Server
        server.start();
    }

    private URI getWebRootResourceUri() throws FileNotFoundException, URISyntaxException {
        URL indexUri = this.getClass().getResource("/webapp");
        if (indexUri == null) {
            throw new FileNotFoundException("Unable to find resource " + "webapp");
        }
        // Points to wherever /webroot/ (the resource) is
        return indexUri.toURI();
    }

    /**
     * Establish Scratch directory for the servlet context (used by JSP compilation)
     */
    private File getScratchDir() throws IOException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File scratchDir = new File(tempDir.toString(), "embedded-jetty-jsp");

        if (!scratchDir.exists()) {
            if (!scratchDir.mkdirs()) {
                throw new IOException("Unable to create scratch directory: " + scratchDir);
            }
        }
        return scratchDir;
    }

    /**
     * Setup the basic application "context" for this application at "/"
     * This is also known as the handler tree (in jetty speak)
     */
    private WebAppContext getWebAppContext(URI baseUri, File scratchDir) {
        WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setAttribute("javax.servlet.context.tempdir", scratchDir);
        context.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*/[^/]*servlet-api-[^/]*\\.jar$|.*/javax.servlet.jsp.jstl-.*\\.jar$|.*/.*taglibs.*\\.jar$");
        context.setResourceBase(baseUri.toASCIIString());
        context.setAttribute("org.eclipse.jetty.containerInitializers", jspInitializers());
        context.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());
        context.addBean(new ServletContainerInitializersStarter(context), true);
        context.setClassLoader(getUrlClassLoader());

        context.addServlet(jspServletHolder(), "*.jsp");

        context.addServlet(defaultServletHolder(baseUri), "/");
        return context;
    }

    /**
     * Ensure the jsp engine is initialized correctly
     */
    private List<ContainerInitializer> jspInitializers() {
        JettyJasperInitializer sci = new JettyJasperInitializer();
        ContainerInitializer initializer = new ContainerInitializer(sci, null);
        List<ContainerInitializer> initializers = new ArrayList<ContainerInitializer>();
        initializers.add(initializer);
        return initializers;
    }

    /**
     * Set Classloader of Context to be sane (needed for JSTL)
     * JSP requires a non-System classloader, this simply wraps the
     * embedded System classloader in a way that makes it suitable
     * for JSP to use
     */
    private ClassLoader getUrlClassLoader() {
        ClassLoader jspClassLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader());
        return jspClassLoader;
    }

    /**
     * Create JSP Servlet (must be named "jsp")
     */
    private ServletHolder jspServletHolder() {
        ServletHolder holderJsp = new ServletHolder("jsp", JettyJspServlet.class);
        holderJsp.setInitOrder(0);
        holderJsp.setInitParameter("logVerbosityLevel", "DEBUG");
        holderJsp.setInitParameter("fork", "false");
        holderJsp.setInitParameter("xpoweredBy", "false");
//        holderJsp.setInitParameter("compilerTargetVM", "1.7");
//        holderJsp.setInitParameter("compilerSourceVM", "1.7");
        holderJsp.setInitParameter("keepgenerated", "true");
        return holderJsp;
    }

    /**
     * Create Default Servlet (must be named "default")
     */
    private ServletHolder defaultServletHolder(URI baseUri) {
        ServletHolder holderDefault = new ServletHolder("default", DefaultServlet.class);
        holderDefault.setInitParameter("resourceBase", baseUri.toASCIIString());
        holderDefault.setInitParameter("dirAllowed", "true");
        return holderDefault;
    }

}
