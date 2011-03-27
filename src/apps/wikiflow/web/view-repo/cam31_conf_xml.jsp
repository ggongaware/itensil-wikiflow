<%@ page import="itensil.web.HTMLEncode,
                 itensil.util.Check,
                 itensil.util.UriHelper,
                 java.util.regex.Pattern,
                 java.util.regex.Matcher,
                 itensil.repository.*" session="false" contentType="text/xml" %>
<%!
    protected static Pattern movRegExp = Pattern.compile(
            "<MovieURL>([^<]+)", Pattern.MULTILINE);
%>
<%
    /**
     * Hack get camtasia URLs localized
     */

    String uri = request.getParameter("url");
    if (Check.isEmpty(uri)  || !uri.endsWith(".xml")) {
        return;
    }
    String pUri = "../fil" + UriHelper.getParent(uri);

    RepositoryHelper.beginTransaction();
    RepositoryHelper.useReadOnly();
    MutableRepositoryNode node = RepositoryHelper.getNode(uri, false);
    NodeContent cont = node.getContent(new DefaultNodeVersion());
    String xmlStr = new String(cont.getBytes());
    RepositoryHelper.commitTransaction();
    RepositoryHelper.closeSession();
    StringBuffer buf = new StringBuffer(xmlStr.length());
    Matcher ma = movRegExp.matcher(xmlStr);
    int last = 0;
    while (ma.find()) {
        buf.append(xmlStr.substring(last, ma.start()));
        String mUrl = ma.group(1);
        buf.append("<MovieURL>");
        buf.append(pUri);
        buf.append('/');
        buf.append(mUrl);
        last = ma.end();
    }
    if (last > 0) {
        buf.append(xmlStr.substring(last));
        out.print(buf);
    }
%>