/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.webconsole.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.NamedChannel;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Error;
import org.jgrapes.core.events.HandlingError;
import org.jgrapes.core.events.Stop;
import org.jgrapes.http.HttpConnector;
import org.jgrapes.http.HttpServer;
import org.jgrapes.http.InMemorySessionManager;
import org.jgrapes.http.LanguageSelector;
import org.jgrapes.http.StaticContentDispatcher;
import org.jgrapes.http.events.Request;
import org.jgrapes.io.FileStorage;
import org.jgrapes.io.NioDispatcher;
import org.jgrapes.io.util.PermitsPool;
import org.jgrapes.net.SocketConnector;
import org.jgrapes.net.SocketServer;
import org.jgrapes.net.SslCodec;
import org.jgrapes.util.ComponentCollector;
import org.jgrapes.util.FileSystemWatcher;
import org.jgrapes.util.YamlConfigurationStore;
import org.jgrapes.util.events.WatchFile;
import org.jgrapes.webconlet.oidclogin.LoginConlet;
import org.jgrapes.webconlet.oidclogin.OidcClient;
import org.jgrapes.webconsole.base.BrowserLocalBackedKVStore;
import org.jgrapes.webconsole.base.ConletComponentFactory;
import org.jgrapes.webconsole.base.ConsoleWeblet;
import org.jgrapes.webconsole.base.KVStoreBasedConsolePolicy;
import org.jgrapes.webconsole.base.PageResourceProviderFactory;
import org.jgrapes.webconsole.base.WebConsole;
import org.jgrapes.webconsole.bootstrap4.Bootstrap4Weblet;
import org.jgrapes.webconsole.jqueryui.JQueryUiWeblet;
import org.jgrapes.webconsole.rbac.RoleConfigurator;
import org.jgrapes.webconsole.rbac.RoleConletFilter;
import org.jgrapes.webconsole.vuejs.VueJsConsoleWeblet;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 *
 */
@SuppressWarnings("PMD.ExcessiveImports")
public class WebConsoleTest extends Component implements BundleActivator {

    private WebConsoleTest app;

    /**
     * Instantiates a new http web console demo.
     */
    public WebConsoleTest() {
        super(new NamedChannel("app"));
    }

    /**
     * Log the exception when a handling error is reported.
     *
     * @param event the event
     */
    @Handler(channels = Channel.class, priority = -10_000)
    @SuppressWarnings("PMD.GuardLogStatement")
    public void onHandlingError(HandlingError event) {
        logger.log(Level.SEVERE, event.throwable(),
            () -> "Problem invoking handler with " + event.event() + ": "
                + event.message());
        event.stop();
    }

    /**
     * Log the exception when a handling error is reported.
     *
     * @param event the event
     */
    @Handler(channels = Channel.class, priority = -11_000)
    @SuppressWarnings("PMD.GuardLogStatement")
    public void onError(Error event) {
        logger.severe(() -> event.toString());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.
     * BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        logger.info(() -> "Starting " + WebConsoleTest.class.getSimpleName());
        // The demo component is the application
        app = new WebConsoleTest();
        // Attach a general nio dispatcher and a file system watcher
        app.attach(new NioDispatcher());
        app.attach(new FileSystemWatcher(app.channel()));

        // Support YAML configuration (and watch it)
        var cfgFile = new File("console-config.yaml");
        app.attach(new YamlConfigurationStore(app.channel(), cfgFile, false));
        app.fire(new WatchFile(cfgFile.toPath()));

        // Network level unencrypted channel.
        Channel guiTransportChannel = new NamedChannel("guiTransport");
        // Create a TCP server listening on port 9888
        app.attach(new SocketServer(guiTransportChannel)
            .setServerAddress(new InetSocketAddress(9888)));

        // Create TLS "converter"
        KeyStore serverStore = KeyStore.getInstance("JKS");
        try (InputStream keyFile
            = Files.newInputStream(Paths.get("demo-resources/localhost.jks"))) {
            serverStore.load(keyFile, "nopass".toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(serverStore, "nopass".toCharArray());
        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(kmf.getKeyManagers(), null, new SecureRandom());
        // Create a TCP server for SSL
        Channel securedNetwork = app.attach(
            new SocketServer().setServerAddress(new InetSocketAddress(5443))
                .setBacklog(3000).setConnectionLimiter(new PermitsPool(50)));
        app.attach(
            new SslCodec(guiTransportChannel, securedNetwork, sslContext));

        // Create an HTTP server as converter between transport and application
        // layer.
        Channel guiHttpChannel = new NamedChannel("guiHttp");
        app.attach(new HttpServer(guiHttpChannel, guiTransportChannel,
            Request.In.Get.class, Request.In.Post.class));

        // Build application layer
        app.attach(new LanguageSelector(guiHttpChannel));
        app.attach(new FileStorage(guiHttpChannel, 65_536));
        app.attach(new StaticContentDispatcher(guiHttpChannel,
            "/**", Paths.get("demo-resources/static-content").toUri()));
        app.attach(new StaticContentDispatcher(guiHttpChannel,
            "/doc|**", Paths.get("../../jgrapes.gh-pages/javadoc").toUri()));
        app.attach(new PostProcessor(guiHttpChannel));
        app.attach(new WsEchoServer(guiHttpChannel));

        // Create network channels for client requests.
        Channel requestChannel = app.attach(new SocketConnector(SELF));
        Channel secReqChannel
            = app.attach(new SslCodec(SELF, requestChannel, true));

        createJQueryUiConsole(guiHttpChannel, requestChannel, secReqChannel);
        createBootstrap4Console(guiHttpChannel, requestChannel, secReqChannel);
        createVueJsConsole(guiHttpChannel, requestChannel, secReqChannel);
        Components.start(app);
    }

    @SuppressWarnings({ "PMD.AvoidDuplicateLiterals",
        "PMD.TooFewBranchesForASwitchStatement" })
    private void createJQueryUiConsole(Channel guiHttpChannel,
            Channel requestChannel, Channel secReqChannel)
            throws URISyntaxException {
        app.attach(new InMemorySessionManager(guiHttpChannel, "/jqconsole")
            .setIdName("id-jq"));
        ConsoleWeblet consoleWeblet
            = app.attach(new JQueryUiWeblet(guiHttpChannel, Channel.SELF,
                new URI("/jqconsole/")))
                .prependResourceBundleProvider(WebConsoleTest.class)
                .prependConsoleResourceProvider(WebConsoleTest.class);
        WebConsole console = consoleWeblet.console();
        consoleWeblet.setConnectionInactivityTimeout(Duration.ofMinutes(5));

        // Support conlets in making HTTP requests
        console.attach(new HttpConnector(console.channel(), requestChannel,
            secReqChannel));

        console.attach(new BrowserLocalBackedKVStore(
            console.channel(), consoleWeblet.prefix().getPath()));
        console.attach(new KVStoreBasedConsolePolicy(console.channel()));
        console.attach(new AvoidEmptyPolicy(console.channel()));
        // Add all available page resource providers
        console.attach(new ComponentCollector<>(
            PageResourceProviderFactory.class, console.channel()));
        // Add all available conlets
        console.attach(new ComponentCollector<>(
            ConletComponentFactory.class, console.channel(), type -> {
                switch (type) {
                default:
                    return Arrays.asList(Collections.emptyMap());
                }
            }));
    }

    @SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
    private void createBootstrap4Console(Channel guiHttpChannel,
            Channel requestChannel, Channel secReqChannel)
            throws URISyntaxException {
        app.attach(new InMemorySessionManager(guiHttpChannel, "/b4console")
            .setIdName("id-b4"));
        ConsoleWeblet consoleWeblet
            = app.attach(new Bootstrap4Weblet(guiHttpChannel, Channel.SELF,
                new URI("/b4console/")))
                .prependClassTemplateLoader(this.getClass())
                .prependResourceBundleProvider(WebConsoleTest.class)
                .prependConsoleResourceProvider(WebConsoleTest.class);
        WebConsole console = consoleWeblet.console();
        consoleWeblet.setConnectionInactivityTimeout(Duration.ofMinutes(5));

        // Support conlets in making HTTP requests
        console.attach(new HttpConnector(console.channel(), requestChannel,
            secReqChannel));

        console.attach(new BrowserLocalBackedKVStore(
            console.channel(), consoleWeblet.prefix().getPath()));
        console.attach(new KVStoreBasedConsolePolicy(console.channel()));
        console.attach(new AvoidEmptyPolicy(console.channel()));
        // Add all available page resource providers
        console.attach(new ComponentCollector<>(
            PageResourceProviderFactory.class, console.channel(),
            type -> {
                switch (type) {
                case "org.jgrapes.webconsole.provider.gridstack.GridstackProvider":
                    return Arrays.asList(
                        Map.of("configuration", "CoreWithJQUiPlugin",
                            "requireTouchPunch", true));
                default:
                    return Arrays.asList(Collections.emptyMap());
                }
            }));
        // Add all available conlets
        console.attach(new ComponentCollector<>(
            ConletComponentFactory.class, console.channel(), type -> {
                switch (type) {
                default:
                    return Arrays.asList(Collections.emptyMap());
                }
            }));
    }

    @SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
    private void createVueJsConsole(Channel guiHttpChannel,
            Channel requestChannel, Channel secReqChannel)
            throws URISyntaxException, IOException {
        app.attach(new InMemorySessionManager(guiHttpChannel, "/vjconsole")
            .setIdName("id-vj"));
        ConsoleWeblet consoleWeblet
            = app.attach(new VueJsConsoleWeblet(guiHttpChannel, Channel.SELF,
                new URI("/vjconsole/")))
                .prependClassTemplateLoader(this.getClass())
                .prependResourceBundleProvider(WebConsoleTest.class)
                .prependConsoleResourceProvider(WebConsoleTest.class);
        WebConsole console = consoleWeblet.console();
        consoleWeblet.setConnectionInactivityTimeout(Duration.ofMinutes(5));

        // Support conlets in making HTTP requests
        console.attach(new HttpConnector(console.channel(), requestChannel,
            secReqChannel));

        // More components
        console.attach(new BrowserLocalBackedKVStore(
            console.channel(), consoleWeblet.prefix().getPath()));
        console.attach(new KVStoreBasedConsolePolicy(console.channel()));
        console.attach(new AvoidEmptyPolicy(console.channel()));
        console.attach(new RoleConfigurator(console.channel()));
        console.attach(new RoleConletFilter(console.channel()));
        console.attach(new LoginConlet(console.channel()));
        console.attach(new OidcClient(console.channel(), guiHttpChannel,
            new URI("/vjconsole/oauth/callback"), 1500));
        // Add all available page resource providers
        console.attach(new ComponentCollector<>(
            PageResourceProviderFactory.class, console.channel(),
            type -> {
                switch (type) {
                case "org.jgrapes.webconsole.provider.gridstack.GridstackProvider":
                    return Arrays.asList(
                        Map.of("requireTouchPunch", true,
                            "configuration", "CoreWithJQUiPlugin"));
                default:
                    return Arrays.asList(Collections.emptyMap());
                }
            }));
        // Add all available conlets
        console.attach(new ComponentCollector<>(
            ConletComponentFactory.class, console.channel(), type -> {
                switch (type) {
                default:
                    return Arrays.asList(Collections.emptyMap());
                }
            }));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        app.fire(new Stop(), Channel.BROADCAST);
        Components.awaitExhaustion();
    }

    /**
     * @param args
     * @throws IOException
     * @throws InterruptedException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws UnrecoverableKeyException
     * @throws CertificateException
     * @throws KeyManagementException
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public static void main(String[] args) throws Exception {
        new WebConsoleTest().start(null);
    }

}
