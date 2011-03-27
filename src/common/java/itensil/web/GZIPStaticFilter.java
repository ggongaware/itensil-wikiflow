/*
 * Copyright 2004-2007 by Itensil, Inc.,
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information
 * of Itensil, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Itensil.
 */
package itensil.web;


import itensil.io.StreamUtil;
import itensil.util.UriHelper;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.zip.GZIPOutputStream;
import java.sql.Date;

/**
 * Only for static files
 *
 * @author  ggongaware@itensil.com
 * @version $Revision: 1.2 $
 *
 * Last updated by $Author: grant $
 */
public class GZIPStaticFilter implements Filter {

    protected HashMap gzTmpFiles;
    protected ServletContext context;

    public GZIPStaticFilter() {
        gzTmpFiles = new HashMap();
    }

    public void init(FilterConfig config) throws ServletException {
        context = config.getServletContext();
    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        if (req instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest)req;
            HttpServletResponse response = (HttpServletResponse)res;
            String ae = request.getHeader("accept-encoding");
            String uri = "/" + UriHelper.localizeUri(request.getContextPath(), request.getRequestURI());
            
            if (!uri.endsWith(".jsp")) {
                if (ae != null && ae.indexOf("gzip") != -1) {
                    String file = context.getRealPath(uri);
                    File reqFil = new File(file);
                    if (!reqFil.exists()) {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                    long lastMod = reqFil.lastModified();
                    Date modDate = new Date(lastMod);
                    if (ServletUtil.isModified(request, modDate)) {
                        GzFile tmpFil = (GzFile)gzTmpFiles.get(uri);
                        if (tmpFil == null || tmpFil.createTime <= (lastMod + 1000)) {
                            tmpFil = new GzFile();
                            tmpFil.tempFile = StreamUtil.tempFile();
                            tmpFil.createTime = System.currentTimeMillis();
                            OutputStream tmpOut = tmpFil.tempFile.getOutput();
                            GZIPOutputStream gzOut = new GZIPOutputStream(tmpOut);
                            StreamUtil.copyStream(new FileInputStream(reqFil), gzOut);
                            gzOut.finish();
                            tmpOut.close();
                        }
                        response.addHeader("Content-Type", context.getMimeType(uri));
                        response.addDateHeader("Last-Modified", lastMod);
                        response.addHeader("Content-Length", Long.toString(tmpFil.tempFile.getSize()));
                        response.addHeader("Content-Encoding", "gzip");
                        OutputStream srvOut = response.getOutputStream();
                        StreamUtil.copyStream(tmpFil.tempFile.getStream(), srvOut);
                        srvOut.close();
                        return;
                    } else {
                        response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
                        return;
                    }
                }
            }
        }
        chain.doFilter(req, res);
    }

    public void destroy() {

    }

    protected static class GzFile {
        StreamUtil.TempFile tempFile;
        long createTime;
    }
}
