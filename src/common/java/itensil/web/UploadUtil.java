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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.oreilly.servlet.multipart.FilePart;
import com.oreilly.servlet.multipart.MultipartParser;
import com.oreilly.servlet.multipart.ParamPart;
import com.oreilly.servlet.multipart.Part;

/**
 * @author ggongaware@itensil.com
 */
public class UploadUtil {

	private Map<String,String> parameters;
    private Map<String,File> files;

	public UploadUtil() {
		parameters = new HashMap<String,String>();
        files = new HashMap<String,File>();

	}

	public void saveUploads(HttpServletRequest request)
	   throws IOException {

		// 8 Megabyte file Limit plus a little fudge for parameters
		MultipartParser parser = new MultipartParser(request, 8008 * 1024);
		Part part;
		while ((part = parser.readNextPart()) != null) {
			if (part.isParam()) {
				ParamPart param = (ParamPart)part;
				parameters.put(param.getName(), param.getStringValue());
			} else if (part.isFile()) {
				FilePart file = (FilePart)part;
				if (file.getFileName() != null) {
					File tmp = File.createTempFile("itenUP",".tmp");
                    tmp.deleteOnExit();
					file.writeTo(tmp);
					files.put(file.getFileName(), tmp);
				}
			}
		}
	}

	public Map<String,File> getFiles() {
        return files;
	}

	public Map<String,String> getParameterMap() {
		return parameters;
	}

    /*
     * @see java.lang.Object#finalize()
     */
    protected void finalize() throws Throwable {
        for (File tmp : files.values()) {
            tmp.delete();
        }
    }

}
