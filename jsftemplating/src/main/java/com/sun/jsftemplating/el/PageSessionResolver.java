/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation. All rights reserved.
 * Copyright (c) 2006, 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package com.sun.jsftemplating.el;

import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.PropertyNotFoundException;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.FacesContext;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * This <code>ELResolver</code> exists to resolve "page session" attributes. This concept, borrowed from
 * NetDynamics / JATO, stores data w/ the page so that it is available throughout the life of the page. This is longer
 * than request scope, but usually shorter than session. This implementation stores the attributes on the
 * <code>UIViewRoot</code>.
 * </p>
 *
 * @author Ken Paulsen (ken.paulsen@sun.com)
 */
public class PageSessionResolver extends ELResolver {

    /**
     * <p>
     * The name an expression must use when it explicitly specifies page session. ("pageSession")
     * </p>
     */
    public static final String PAGE_SESSION = "pageSession";

    /**
     * <p>
     * The attribute key in which to store the "page" session Map.
     * </p>
     */
    private static final String PAGE_SESSION_KEY = "_ps";

    /**
     * <p>
     * Checks "page session" to see if the value exists.
     * </p>
     */
    @Override
    public Object getValue(ELContext elContext, Object base, Object property) {
        if (base != null) {
            return null;
        }

        if (property == null) {
            throw new PropertyNotFoundException();
        }

        FacesContext facesContext = (FacesContext) elContext.getContext(FacesContext.class);
        UIViewRoot viewRoot = facesContext.getViewRoot();
        Map<String, Serializable> pageSession = getPageSession(facesContext, viewRoot);

        Object value = null;
        // Check to see if expression explicitly asks for PAGE_SESSION
        if (property.equals(PAGE_SESSION)) {
            // It does, return the Map
            if (pageSession == null) {
                // No Map! That's ok, create one...
                pageSession = createPageSession(facesContext, viewRoot);
            }
            value = pageSession;
        } else {
            if (pageSession != null) {
                // Check page session
                value = pageSession.get(property.toString());
            }
        }

        if (value != null || (pageSession != null && pageSession.containsKey(property.toString()))) {
            elContext.setPropertyResolved(true);
        }

        return value;
    }

    @Override
    public Class<?> getType(ELContext elContext, Object base, Object property) {
        checkPropertyFound(base, property);
        return null;
    }

    @Override
    public void setValue(ELContext elContext, Object base, Object property, Object value) {
        checkPropertyFound(base, property);
    }

    @Override
    public boolean isReadOnly(ELContext elContext, Object base, Object property) {
        checkPropertyFound(base, property);
        return false;
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext elContext, Object base) {
        return base == null ? String.class : null;
    }

    /**
     * <p>
     * This method provides access to the "page session" <code>Map</code>. If it doesn't exist, it returns
     * <code>null</code>. If the given <code>UIViewRoot</code> is null, then the current <code>UIViewRoot</code> will be
     * used.
     * </p>
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Serializable> getPageSession(FacesContext facesContext, UIViewRoot viewRoot) {
        if (viewRoot == null) {
            viewRoot = facesContext.getViewRoot();
        }
        return (Map<String, Serializable>) viewRoot.getAttributes().get(PAGE_SESSION_KEY);
    }

    /**
     * <p>
     * This method will create a new "page session" <code>Map</code>. It will overwrite any existing "page session"
     * <code>Map</code>, so be careful.
     * </p>
     */
    public static Map<String, Serializable> createPageSession(FacesContext facesContext, UIViewRoot viewRoot) {
        if (viewRoot == null) {
            viewRoot = facesContext.getViewRoot();
        }

        // Create it...
        Map<String, Serializable> pageSession = new HashMap<>(4);

        // Store it...
        viewRoot.getAttributes().put(PAGE_SESSION_KEY, pageSession);

        // Return it...
        return pageSession;
    }

    private static void checkPropertyFound(Object base, Object property) {
        if (base == null && property == null) {
            throw new PropertyNotFoundException();
        }
    }
}
