/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.portlets.sysinfo;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;

import java.beans.ConstructorProperties;
import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.portal.PortalSession;
import org.jgrapes.portal.PortalWeblet;

import static org.jgrapes.portal.Portlet.RenderMode;

import org.jgrapes.portal.events.AddPageResources.ScriptResource;
import org.jgrapes.portal.events.AddPortletRequest;
import org.jgrapes.portal.events.AddPortletType;
import org.jgrapes.portal.events.DeletePortlet;
import org.jgrapes.portal.events.DeletePortletRequest;
import org.jgrapes.portal.events.NotifyPortletView;
import org.jgrapes.portal.events.PortalReady;
import org.jgrapes.portal.events.RenderPortletRequest;
import org.jgrapes.portal.events.RenderPortletRequestBase;
import org.jgrapes.portal.freemarker.FreeMarkerPortlet;

/**
 * 
 */
public class SysInfoPortlet extends FreeMarkerPortlet {

	private static final Set<RenderMode> MODES = RenderMode.asSet(
			RenderMode.DeleteablePreview, RenderMode.View);
	
	/**
	 * The periodically generated update event.
	 */
	public static class Update extends Event<Void> {
	}
	
	/**
	 * Creates a new component with its channel set to the given 
	 * channel.
	 * 
	 * @param componentChannel the channel that the component's 
	 * handlers listen on by default and that 
	 * {@link Manager#fire(Event, Channel...)} sends the event to 
	 */
	public SysInfoPortlet(Channel componentChannel) {
		super(componentChannel, true);
		setPeriodicRefresh(Duration.ofSeconds(1), () -> new Update());
	}

	/**
	 * On {@link PortalReady}, fire the {@link AddPortletType}.
	 *
	 * @param event the event
	 * @param portalSession the portal session
	 * @throws TemplateNotFoundException the template not found exception
	 * @throws MalformedTemplateNameException the malformed template name exception
	 * @throws ParseException the parse exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Handler
	public void onPortalReady(PortalReady event, PortalSession portalSession) 
			throws TemplateNotFoundException, MalformedTemplateNameException, 
			ParseException, IOException {
		ResourceBundle resourceBundle = resourceBundle(portalSession.locale());
		// Add SysInfoPortlet resources to page
		portalSession.respond(new AddPortletType(type())
				.setDisplayName(resourceBundle.getString("portletName"))
				.addScript(new ScriptResource()
						.setRequires(new String[] {"chartjs.org"})
						.setScriptUri(event.renderSupport().portletResource(
								type(), "SysInfo-functions.ftl.js")))
				.addCss(event.renderSupport(), PortalWeblet.uriFromPath(
						"SysInfo-style.css"))
				.setInstantiable());
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#generatePortletId()
	 */
	@Override
	protected String generatePortletId() {
		return type() + "-" + super.generatePortletId();
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#modelFromSession
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected <T extends Serializable> Optional<T> stateFromSession(
			Session session, String portletId, Class<T> type) {
		if (portletId.startsWith(type() + "-")) {
			return Optional.of((T)new SysInfoModel(portletId));
		}
		return Optional.empty();
	}

	@Override
	public String doAddPortlet(AddPortletRequest event,
			PortalSession portalSession) throws Exception {
		String portletId = generatePortletId();
		SysInfoModel portletModel = putInSession(
				portalSession.browserSession(), new SysInfoModel(portletId));
		renderPortlet(event, portalSession, portletModel);
		return portletId;
	}
	
	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#doRenderPortlet
	 */
	@Override
	protected void doRenderPortlet(RenderPortletRequest event,
	        PortalSession portalSession, String portletId, 
	        Serializable retrievedState) throws Exception {
		SysInfoModel portletModel = (SysInfoModel)retrievedState;
		renderPortlet(event, portalSession, portletModel);
	}

	@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
	private void renderPortlet(RenderPortletRequestBase<?> event,
	        PortalSession portalSession, SysInfoModel portletModel)
	        throws TemplateNotFoundException, MalformedTemplateNameException,
	        ParseException, IOException {
		switch (event.renderMode()) {
		case Preview:
		case DeleteablePreview: {
			Template tpl = freemarkerConfig().getTemplate("SysInfo-preview.ftl.html");
			portalSession.respond(new RenderPortletFromTemplate(event,
					SysInfoPortlet.class, portletModel.getPortletId(), 
					tpl, fmModel(event, portalSession, portletModel))
					.setRenderMode(RenderMode.DeleteablePreview)
					.setSupportedModes(MODES)
					.setForeground(event.isForeground()));
			updateView(portalSession, portletModel.getPortletId());
			break;
		}
		case View: {
			Template tpl = freemarkerConfig().getTemplate("SysInfo-view.ftl.html");
			portalSession.respond(new RenderPortletFromTemplate(event,
					SysInfoPortlet.class, portletModel.getPortletId(), 
					tpl, fmModel(event, portalSession, portletModel))
					.setSupportedModes(MODES).setForeground(event.isForeground()));
			break;
		}
		default:
			break;
		}
	}

	private void updateView(PortalSession portalSession, String portletId) {
		if (!portalSession.isConnected()) {
			return;
		}
		Runtime runtime = Runtime.getRuntime();
		portalSession.respond(new NotifyPortletView(type(),
				portletId, "updateMemorySizes", 
				System.currentTimeMillis(), runtime.maxMemory(),
				runtime.totalMemory(), 
				runtime.totalMemory() - runtime.freeMemory()));
	}
	
	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#doDeletePortlet
	 */
	@Override
	protected void doDeletePortlet(DeletePortletRequest event,
	        PortalSession portalSession, String portletId, 
	        Serializable retrievedState) throws Exception {
		portalSession.respond(new DeletePortlet(portletId));
	}

	/**
	 * Handle the periodic update event by sending {@link NotifyPortletView}
	 * events.
	 *
	 * @param event the event
	 * @param portalSession the portal session
	 */
	@Handler
	public void onUpdate(Update event, PortalSession portalSession) {
		Set<String> portletIds = portletIdsByPortalSession().get(portalSession);
		for (String portletId: portletIds) {
			updateView(portalSession, portletId);			
		}
	}
	
	/**
	 * The portlet's model.
	 */
	@SuppressWarnings("serial")
	public static class SysInfoModel extends PortletBaseModel {

		/**
		 * Creates a new model with the given type and id.
		 * 
		 * @param portletId the portlet id
		 */
		@ConstructorProperties({"portletId"})
		public SysInfoModel(String portletId) {
			super(portletId);
		}

		/**
		 * Return the system properties.
		 *
		 * @return the properties
		 */
		public Properties systemProperties() {
			return System.getProperties();
		}
		
		/**
		 * Return the {@link Runtime}.
		 *
		 * @return the runtime
		 */
		public Runtime runtime() {
			return Runtime.getRuntime();
		}
	}
	
}
