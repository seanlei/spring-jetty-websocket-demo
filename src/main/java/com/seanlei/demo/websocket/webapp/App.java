package com.seanlei.demo.websocket.webapp;

import com.seanlei.demo.websocket.config.AppConfig;
import com.seanlei.demo.websocket.config.WebsocketConfig;
import org.eclipse.jetty.server.Server;
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

import java.net.URL;
import java.util.EventListener;

/**
 * Created by Sean Lei on 6/20/16.
 */
@Component
public class App {
    @Value("${server.port}")
    private int serverPort;

    public void start(WebApplicationContext webCxt) {
        WebAppContext contextHandler = new WebAppContext();

//        ServletContextHandler contextHandler = new ServletContextHandler();
        contextHandler.setContextPath("/");

        DispatcherServlet dispatcherServlet = new DispatcherServlet(webCxt);
        Server server = new Server(serverPort);

        String baseStr = "/webapp";
        URL baseUrl = App.class.getResource(baseStr);
        String basePath = baseUrl.toExternalForm();

        contextHandler.addServlet(new ServletHolder(dispatcherServlet), "/");
        contextHandler.setEventListeners(new EventListener[]{new ContextLoaderListener(webCxt)});
        contextHandler.setResourceBase(basePath);

        //设置jsp的支持
        Configuration.ClassList classlist = Configuration.ClassList
                .setServerDefault(server);
        classlist.addBefore(
                "org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
                "org.eclipse.jetty.annotations.AnnotationConfiguration");
        contextHandler.setAttribute(
                "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
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
        app.start(webCxt);
    }
}
