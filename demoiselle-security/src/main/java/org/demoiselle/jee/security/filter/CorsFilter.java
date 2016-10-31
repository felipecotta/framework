/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.demoiselle.jee.security.filter;

import java.io.IOException;
import java.lang.reflect.Method;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import org.demoiselle.jee.security.DemoiselleSecurityConfig;
import org.demoiselle.jee.security.annotation.Cors;
import org.demoiselle.jee.security.annotation.NoCors;

/**
 *
 * @author 70744416353
 */
@Provider
public class CorsFilter implements ContainerResponseFilter {

    @Inject
    private DemoiselleSecurityConfig config;

    @Context
    private ResourceInfo info;

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) throws IOException {
        Method method = info.getResourceMethod();

        res.getHeaders().putSingle("Authorization", "enabled");
        res.getHeaders().putSingle("x-content-type-options", "nosniff");
        res.getHeaders().putSingle("x-frame-options", "SAMEORIGIN");
        res.getHeaders().putSingle("x-xss-protection", "1; mode=block");

        if (method != null) {
            if (config.isCorsEnabled()) {
                NoCors nocors = method.getAnnotation(NoCors.class);
                if (nocors != null) {
                    res.getHeaders().remove("Access-Control-Allow-Origin");
                    res.getHeaders().remove("Access-Control-Allow-Methods");
                } else {
                    res.getHeaders().putSingle("Access-Control-Allow-Origin", "*");
                    res.getHeaders().putSingle("Access-Control-Allow-Headers", "Origin, Content-type, Accept, Authorization");
                    res.getHeaders().putSingle("Access-Control-Allow-Credentials", "true");
                    res.getHeaders().putSingle("Access-Control-Allow-Methods", req.getMethod());
                }
            } else {
                Cors cors = method.getAnnotation(Cors.class);
                if (cors != null) {
                    res.getHeaders().putSingle("Access-Control-Allow-Origin", "*");
                    res.getHeaders().putSingle("Access-Control-Allow-Headers", "Origin, Content-type, Accept, Authorization");
                    res.getHeaders().putSingle("Access-Control-Allow-Credentials", "true");
                    res.getHeaders().putSingle("Access-Control-Allow-Methods", req.getMethod());
                } else {
                    res.getHeaders().remove("Access-Control-Allow-Origin");
                    res.getHeaders().remove("Access-Control-Allow-Methods");
                }
            }
        }
    }

}