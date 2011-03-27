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
package itensil.config;

import itensil.util.Check;
import itensil.util.UriHelper;

import java.util.HashMap;

import javax.servlet.ServletContext;

public class MimeMapper {

	ServletContext sctx;
	HashMap<String,String> map = new HashMap<String,String>();
	
	public MimeMapper() {
		this.sctx = null;
	}
	
	public MimeMapper(ServletContext sctx) {
		this.sctx = sctx;
	}
	
	public String getMimeType(String uri) {
		String ext = UriHelper.getExtension(uri);
		String mime = map.get(ext);
		if (Check.isEmpty(mime) && sctx != null) {
			mime = sctx.getMimeType(uri);
		}
		if (Check.isEmpty(mime)) {
			mime = "text/xml";
		}
		return mime;
	}
	
}
