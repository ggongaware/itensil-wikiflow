package itensil.entities;

import itensil.repository.RepositoryNode;
import itensil.repository.filter.NodeFilter;
import itensil.util.UriHelper;

public class EntityDataNodeFilter implements NodeFilter {

	public boolean accept(RepositoryNode node) {
		if (!node.isCollection()) {
			String uri = node.getUri();
			return "data.xml".equals(UriHelper.name(uri)) 
				&& "records".equals(UriHelper.name(UriHelper.getParent(UriHelper.getParent(uri))));
		}
		return false;
	}

}
