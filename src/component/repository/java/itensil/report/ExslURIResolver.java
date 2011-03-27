package itensil.report;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;

import itensil.io.xml.SAXHandler;
import itensil.repository.AccessDeniedException;
import itensil.repository.LockException;
import itensil.repository.MutableRepositoryNode;
import itensil.repository.NotFoundException;
import itensil.repository.RepositoryHelper;
import itensil.util.Check;
import itensil.util.UriHelper;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ExslURIResolver implements URIResolver {
	
	String baseUri;
	HashMap<String, MutableRepositoryNode> sourceCache;

	public ExslURIResolver(String baseUri) {
		this.baseUri = baseUri;
		sourceCache = new HashMap<String, MutableRepositoryNode>();
	}
	
	public Source resolve(String href, String base) throws TransformerException {
		Source sXsl = null;
		try {
			if (!Check.isEmpty(href)) {
				if (isNetUri(href)) {
					sXsl = new SAXSource(
					        new ExslSandBoxReader((new SAXHandler()).getParser().getXMLReader()),
					        new InputSource(href));
				} else if (!Check.isEmpty(base) && isNetUri(base)) {
					URL url = new URL(new URL(base), href);
					sXsl = new SAXSource(
					        new ExslSandBoxReader((new SAXHandler()).getParser().getXMLReader()),
					        new InputSource(url.toString()));
				} else {
					MutableRepositoryNode node = sourceCache.get(href);
					if (node == null) {
						String uri = UriHelper.absoluteUri(baseUri, href);
						uri = RepositoryHelper.resolveUri(uri);
						node = RepositoryHelper.getNode(uri, false);
						sourceCache.put(href, node);
					}
					sXsl = new SAXSource(
					        new ExslSandBoxReader((new SAXHandler()).getParser().getXMLReader()),
					        new InputSource(RepositoryHelper.loadContent(node)));
				}
			}
		} catch (SAXException e) {
			throw new TransformerException(e);
		} catch (AccessDeniedException e) {
			throw new TransformerException(e);
		} catch (NotFoundException e) {
			throw new TransformerException(e);
		} catch (LockException e) {
			throw new TransformerException(e);
		} catch (MalformedURLException e) {
			throw new TransformerException(e);
		}
		return sXsl;
	}
	
	protected boolean isNetUri(String uri) {
		return uri.startsWith("http:") || uri.startsWith("https:") || uri.startsWith("ftp:");
	}

}
