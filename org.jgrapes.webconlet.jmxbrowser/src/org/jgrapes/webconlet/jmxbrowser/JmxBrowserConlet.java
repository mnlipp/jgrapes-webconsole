/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2021  Michael N. Lipp
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

package org.jgrapes.webconlet.jmxbrowser;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.jdrupes.json.JsonBeanEncoder;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.util.events.KeyValueStoreUpdate;
import org.jgrapes.webconsole.base.AbstractConlet;
import org.jgrapes.webconsole.base.Conlet.RenderMode;
import org.jgrapes.webconsole.base.ConsoleSession;
import org.jgrapes.webconsole.base.WebConsoleUtils;
import org.jgrapes.webconsole.base.events.AddConletRequest;
import org.jgrapes.webconsole.base.events.AddConletType;
import org.jgrapes.webconsole.base.events.AddPageResources.ScriptResource;
import org.jgrapes.webconsole.base.events.ConsoleReady;
import org.jgrapes.webconsole.base.events.DisplayNotification;
import org.jgrapes.webconsole.base.events.NotifyConletModel;
import org.jgrapes.webconsole.base.events.NotifyConletView;
import org.jgrapes.webconsole.base.events.RenderConletRequest;
import org.jgrapes.webconsole.base.events.RenderConletRequestBase;
import org.jgrapes.webconsole.base.freemarker.FreeMarkerConlet;

public class JmxBrowserConlet
        extends FreeMarkerConlet<JmxBrowserConlet.JmxBrowserModel> {

    private static final Set<RenderMode> MODES = RenderMode.asSet(
        RenderMode.Preview, RenderMode.View);

    private static MBeanServer mbeanServer
        = ManagementFactory.getPlatformMBeanServer();

    /**
     * Creates a new component with its channel set to the given channel.
     * 
     * @param componentChannel the channel that the component's handlers listen
     *            on by default and that {@link Manager#fire(Event, Channel...)}
     *            sends the event to
     */
    public JmxBrowserConlet(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * On {@link ConsoleReady}, fire the {@link AddConletType}.
     *
     * @param event the event
     * @param channel the channel
     * @throws TemplateNotFoundException the template not found exception
     * @throws MalformedTemplateNameException the malformed template name
     *             exception
     * @throws ParseException the parse exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler
    public void onConsoleReady(ConsoleReady event, ConsoleSession channel)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        // Add conlet resources to page
        channel.respond(new AddConletType(type())
            .setDisplayNames(
                localizations(channel.supportedLocales(), "conletName"))
            .addScript(new ScriptResource()
                .setScriptUri(event.renderSupport().conletResource(
                    type(), "jmxbrowser.min.js"))
                .setScriptType("module")));
    }

    @Override
    protected String generateConletId() {
        return type() + "-" + super.generateConletId();
    }

    @Override
    protected Optional<JmxBrowserModel> stateFromSession(
            Session session, String conletId) {
        if (conletId.startsWith(type() + "-")) {
            return Optional.of(new JmxBrowserModel(conletId));
        }
        return Optional.empty();
    }

    @Override
    protected ConletTrackingInfo doAddConlet(AddConletRequest event,
            ConsoleSession channel)
            throws Exception {
        JmxBrowserModel conletModel
            = new JmxBrowserModel(generateConletId());
        return new ConletTrackingInfo(conletModel.getConletId())
            .addModes(renderConlet(event, channel, conletModel));
    }

    @Override
    protected Set<RenderMode> doRenderConlet(RenderConletRequest event,
            ConsoleSession channel, String conletId,
            JmxBrowserModel conletModel)
            throws Exception {
        return renderConlet(event, channel, conletModel);
    }

    private Set<RenderMode> renderConlet(RenderConletRequestBase<?> event,
            ConsoleSession channel, JmxBrowserModel conletModel)
            throws TemplateNotFoundException,
            MalformedTemplateNameException, ParseException, IOException {
        Set<RenderMode> renderedAs = new HashSet<>();
        if (event.renderAs().contains(RenderMode.Preview)) {
            Template tpl = freemarkerConfig()
                .getTemplate("JmxBrowser-preview.ftl.html");
            channel.respond(new RenderConletFromTemplate(event,
                type(), conletModel.getConletId(), tpl,
                fmModel(event, channel, conletModel))
                    .setRenderAs(
                        RenderMode.Preview.addModifiers(event.renderAs()))
                    .setSupportedModes(MODES));
            channel.respond(new NotifyConletView(type(),
                conletModel.getConletId(), "mbeansTree", genMBeansTree(),
                "preview", true));
            renderedAs.add(RenderMode.Preview);
        }
        if (event.renderAs().contains(RenderMode.View)) {
            Template tpl
                = freemarkerConfig().getTemplate("JmxBrowser-view.ftl.html");
            channel.respond(new RenderConletFromTemplate(event,
                type(), conletModel.getConletId(), tpl,
                fmModel(event, channel, conletModel))
                    .setRenderAs(
                        RenderMode.View.addModifiers(event.renderAs()))
                    .setSupportedModes(MODES));
            channel.respond(new NotifyConletView(type(),
                conletModel.getConletId(), "mbeansTree", genMBeansTree(),
                "view", true));
            renderedAs.add(RenderMode.View);
        }
        return renderedAs;
    }

    @Override
    protected void doNotifyConletModel(NotifyConletModel event,
            ConsoleSession channel, JmxBrowserModel conletModel)
            throws Exception {
        event.stop();

//        String jsonState = JsonBeanEncoder.create()
//            .writeObject(conletModel).toJson();
//        channel.respond(new KeyValueStoreUpdate().update(
//            storagePath(channel.browserSession()) + conletModel.getConletId(),
//            jsonState));
//        channel.respond(new NotifyConletView(type(),
//            conletModel.getConletId(), "setWorldVisible",
//            conletModel.isWorldVisible()));
//        channel.respond(new DisplayNotification("<span>"
//            + resourceBundle(channel.locale()).getString("visibilityChange")
//            + "</span>")
//                .addOption("autoClose", 2000));
    }

    public static class NodeDTO {
        public String segment;
        public String label;
        public Set<NodeDTO> children;

        public NodeDTO(String segment, String label, Set<NodeDTO> children) {
            this.segment = segment;
            this.label = label;
            this.children = children;
        }

        public NodeDTO(String segment, String label) {
            this(segment, label,
                new TreeSet<>(Comparator.comparing((node) -> node.label)));
        }

        public String getSegment() {
            return segment;
        }

        public String getLabel() {
            return label;
        }

        public Set<NodeDTO> getChildren() {
            return children;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result
                = prime * result + ((segment == null) ? 0 : segment.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            NodeDTO other = (NodeDTO) obj;
            if (segment == null) {
                if (other.segment != null) {
                    return false;
                }
            } else if (!segment.equals(other.segment)) {
                return false;
            }
            return true;
        }
    }

    private List<NodeDTO> genMBeansTree() {
        Map<String, NodeDTO> trees = new TreeMap<>();
        Set<ObjectName> mbeanNames = mbeanServer.queryNames(null, null);
        for (ObjectName mbn : mbeanNames) {
            NodeDTO domainGroup = trees.computeIfAbsent(mbn.getDomain(),
                key -> new NodeDTO(mbn.getDomain(), mbn.getDomain()));
            appendToGroup(domainGroup, "type",
                new Hashtable<>(mbn.getKeyPropertyList()), mbn);
        }
        List<NodeDTO> roots = new ArrayList<>(trees.values());
        return roots;
    }

    private void appendToGroup(NodeDTO parent, String property,
            Hashtable<String, String> propsLeft, ObjectName mbn) {
        if (!propsLeft.keySet().contains(property)) {
            try {
                String left = ObjectName.getInstance("tmp", propsLeft)
                    .getCanonicalKeyPropertyListString();
                parent.children.add(new NodeDTO(left, left, null));
            } catch (MalformedObjectNameException e) {
                // Shoudn't happen
            }
            return;
        }
        Set<NodeDTO> candidates = parent.children;
        String partition = property + "=" + propsLeft.get(property);
        NodeDTO match = candidates.stream()
            .filter(n -> n.segment.equals(partition)).findFirst()
            .orElseGet(() -> {
                NodeDTO node = new NodeDTO(partition, propsLeft.get(property));
                candidates.add(node);
                return node;
            });
        propsLeft.remove(property);
        appendToGroup(match, property.equals("type") ? "name" : "", propsLeft,
            mbn);
    }

    /**
     * The conlet's model.
     */
    @SuppressWarnings("serial")
    public class JmxBrowserModel extends AbstractConlet.ConletBaseModel {

        /**
         * Instantiates a new service list model.
         *
         * @param conletId the conlet id
         */
        public JmxBrowserModel(String conletId) {
            super(conletId);
        }

    }
}
