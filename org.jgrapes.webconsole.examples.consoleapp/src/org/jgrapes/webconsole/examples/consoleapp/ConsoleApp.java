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

package org.jgrapes.webconsole.examples.consoleapp;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collections;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.jgrapes.core.Channel;
import org.jgrapes.core.ClassChannel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.NamedChannel;
import org.jgrapes.core.events.Stop;
import org.jgrapes.http.HttpServer;
import org.jgrapes.http.InMemorySessionManager;
import org.jgrapes.http.LanguageSelector;
import org.jgrapes.http.events.Request;
import org.jgrapes.io.NioDispatcher;
import org.jgrapes.io.util.PermitsPool;
import org.jgrapes.net.SocketServer;
import org.jgrapes.net.SslCodec;
import org.jgrapes.util.ComponentCollector;
import org.jgrapes.util.PreferencesStore;
import org.jgrapes.webconsole.base.BrowserLocalBackedKVStore;
import org.jgrapes.webconsole.base.ConletComponentFactory;
import org.jgrapes.webconsole.base.ConsoleWeblet;
import org.jgrapes.webconsole.base.KVStoreBasedConsolePolicy;
import org.jgrapes.webconsole.base.PageResourceProviderFactory;
import org.jgrapes.webconsole.base.WebConsole;
import org.jgrapes.webconsole.vuejs.VueJsConsoleWeblet;

/**
 *
 */
public class ConsoleApp extends Component {

    private ConsoleApp app;

    /**
     * Instantiates a new http(s) web console demo.
     */
    public ConsoleApp() {
        super(new ClassChannel() {
        });
    }

    /**
     * Start the application.
     *
     * @throws Exception the exception
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public void start() throws Exception {
        // This class represents the application and its instance is
        // therefore the root of the component tree.
        app = new ConsoleApp();
        // Attach a general nio dispatcher
        app.attach(new NioDispatcher());

        // Create a channel for the network level I/O.
        Channel httpTransport = new NamedChannel("httpTransport");

        // Create a TCP server listening on port 8888 that
        // uses (i.e. receives events from and sends events to)
        // the network channel.
        app.attach(new SocketServer(httpTransport)
            .setServerAddress(new InetSocketAddress(8888)));

        // The third component that uses (i.e. receives events from
        // and sends events to) the httpTransport channel is
        // an SslCodec. This actually uses two channels. The second
        // channel is used for the encrypted I/O which is exchanged
        // with another SocketServer.
        SSLContext sslContext = createSslContext();
        Channel securedNetwork = app.attach(
            new SocketServer().setServerAddress(new InetSocketAddress(8443))
                .setBacklog(3000).setConnectionLimiter(new PermitsPool(50)));
        app.attach(new SslCodec(httpTransport, securedNetwork, sslContext));

        // Create an HTTP server as converter between transport and
        // application layer. It connects to the httpTransport channel
        // and the application channel which is provided by the
        // ConsoleApp instance. The HTTP related events are send on/
        // received from the http channel.
        Channel httpApplication = new NamedChannel("http");
        var httpServer = app.attach(new HttpServer(httpApplication,
            httpTransport, Request.In.Get.class, Request.In.Post.class));

        // Build application layer. The application layer events are
        // exchanged over the application channel.
        httpServer.attach(
            new PreferencesStore(httpApplication, this.getClass()));
        httpServer.attach(new InMemorySessionManager(httpApplication));
        httpServer.attach(new LanguageSelector(httpApplication));

        // Assemble console components
        createVueJsConsole(httpServer);
        Components.start(app);
    }

    private SSLContext createSslContext() throws KeyStoreException, IOException,
            NoSuchAlgorithmException, CertificateException,
            UnrecoverableKeyException, KeyManagementException {
        KeyStore serverStore = KeyStore.getInstance("JKS");
        try (InputStream keyFile
            = ConsoleApp.class.getResourceAsStream("localhost.jks")) {
            serverStore.load(keyFile, "nopass".toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(serverStore, "nopass".toCharArray());
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, new SecureRandom());
        return sslContext;
    }

    @SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
    private void createVueJsConsole(HttpServer httpServer)
            throws URISyntaxException {
        ConsoleWeblet consoleWeblet
            = httpServer.attach(new VueJsConsoleWeblet(httpServer.channel(),
                Channel.SELF, new URI("/")))
                .prependClassTemplateLoader(this.getClass())
                .prependResourceBundleProvider(ConsoleApp.class)
                .prependConsoleResourceProvider(ConsoleApp.class);
        WebConsole console = consoleWeblet.console();
        // consoleWeblet.setConnectionInactivityTimeout(300_000);
        console.attach(new BrowserLocalBackedKVStore(
            console.channel(), consoleWeblet.prefix().getPath()));
        console.attach(new KVStoreBasedConsolePolicy(console.channel()));
        // Add all available page resource providers
        console.attach(new ComponentCollector<>(
            PageResourceProviderFactory.class, console.channel()));
        // Add all available conlets
        console.attach(new ComponentCollector<>(
            ConletComponentFactory.class, console.channel(), type -> {
                switch (type) {
                case "org.jgrapes.webconlet.examples.login.LoginConlet":
                    return Collections.emptyList();
                default:
                    return Arrays.asList(Collections.emptyMap());
                }
            }));
    }

    /**
     * Stop the application.
     * @throws Exception 
     *
     * @throws Exception the exception
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public void stop() throws Exception {
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
        new ConsoleApp().start();
    }

}
