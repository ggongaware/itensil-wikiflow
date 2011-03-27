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
package itensil.io;

import java.io.*;

/**
 * <p>Title: Teamlines</p>
 * <p>Description: Collaborative Process Management</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: Itensil, Inc</p>
 * @author ggongaware
 * @version 1.0
 */
public class StreamUtil {
	
	public static class TempFile {
		
		private File tmpFile;
				
		protected TempFile() throws IOException {
			tmpFile = File.createTempFile("bpn",".tmp");
            tmpFile.deleteOnExit();
		}
		
		public InputStream getStream()	throws IOException {
			return new BufferedInputStream(new FileInputStream(tmpFile));
		}
		
		public OutputStream getOutput() throws IOException {
			return new BufferedOutputStream(new FileOutputStream(tmpFile));
		}
		
		public long getSize() {
			return tmpFile.length();
		}	
		
		protected void finalize() throws Throwable {
			tmpFile.delete();
        }
	}

	public static void copyStream(InputStream in, OutputStream out) 
			throws IOException {
	
		byte[] buf = new byte[4 * 1024]; // 4K buf
        int bytesRead;
        while ((bytesRead = in.read(buf)) != -1) {
            out.write(buf, 0, bytesRead);
        }
	}

    public static void copyStream(
        InputStream in, OutputStream out, String find, String replace)
        throws IOException {

        copyStream(
            new InputStreamReader(in),
            new OutputStreamWriter(out),
            find,
            replace);
    }

    public static void copyStream(
        Reader in, Writer out, String find, String replace) throws IOException {

        int fLen = find.length();
        int size = fLen * 2;
        char[] buf = new char[size];
        char[] buf2 = new char[size];
        int charsRead;
        int skip = 0;
        while ((charsRead = in.read(buf, skip, size - skip)) != -1) {
            int count = skip + charsRead;
            if (count >= fLen) {
                String sBuf = new String(buf);
                int pos = sBuf.indexOf(find);
                int offset;
                if (pos >= 0) {
                    out.write(buf, 0, pos);
                    out.write(replace);
                    offset = pos + fLen;
                    skip = count - offset;
                } else {
                    out.write(buf, 0, fLen);
                    offset = fLen;
                    skip = count - fLen;
                    System.arraycopy(buf, fLen, buf2, 0, skip);
                }
                System.arraycopy(buf, offset, buf2, 0, skip);
                char swap[] = buf;
                buf = buf2;
                buf2 = swap;
            } else {
                skip = count;
            }
        }
        if (skip > 0) out.write(buf, 0, skip);
    }

    /**
     * With byte range
     * @param in
     * @param out
     * @param start
     * @param end
     * @throws IOException
     */
    public static void copyStream(
        InputStream in, 
        OutputStream out, 
        long start, 
        long end) 
        throws IOException {
    
        byte[] buf = new byte[4 * 1024]; // 4K buf
        
        // position input stream
        in.skip(start);        
        long bytesLeft = end - start + 1;        
        int bytesRead;
        while ((bytesLeft > 0) && (bytesRead = in.read(buf)) != -1) {
                       
            if (bytesLeft >= bytesRead) {
                out.write(buf, 0, bytesRead);
                bytesLeft -= bytesRead;
            } else {
                out.write(buf, 0, (int)bytesLeft);
                bytesLeft = 0;
            }            
        }
    }

    public static void copyStream(InputStream in, Writer out)
            throws IOException {
        copyStream(new InputStreamReader(in), out);
    }


    public static void copyStream(Reader in, Writer out) throws IOException {
        char[] buf = new char[4 * 1024]; // 4K buf
        int charsRead;
        while ((charsRead = in.read(buf)) != -1) {
            out.write(buf, 0, charsRead);
        }
    }
    
    public static String streamToString(InputStream in) throws IOException {
    	StringWriter out = new StringWriter();
    	copyStream(in, out);
    	return out.toString();
    }

	public static TempFile tempFile() throws IOException {
		return new TempFile();
	}
	
	public static TempFile toTempFile(InputStream in) throws IOException {
		TempFile tf = new TempFile();
		FileOutputStream out = new FileOutputStream(tf.tmpFile);
		copyStream(in, out);
		out.close();
		return tf;
	}

    public static TempFile toTempFile(
        InputStream in, String find, String replace) throws IOException {
		TempFile tf = new TempFile();
        FileWriter out = new FileWriter(tf.tmpFile);
		copyStream(new InputStreamReader(in), out, find, replace);
		out.close();
		return tf;
	}

	public static TempFile toTempFile(InputStream in, ReplaceFilter filter)
			throws IOException {
		TempFile tf = new TempFile();
		FileOutputStream out = new FileOutputStream(tf.tmpFile);
		filter.execute(in, out);
		out.close();
		return tf;
	}
}
