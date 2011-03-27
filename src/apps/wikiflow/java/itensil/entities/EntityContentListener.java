package itensil.entities;

import org.apache.log4j.Logger;

import itensil.io.xml.XMLDocument;
import itensil.repository.AccessDeniedException;
import itensil.repository.MutableRepositoryNode;
import itensil.repository.NodeContent;
import itensil.repository.RepositoryHelper;
import itensil.repository.event.ContentChangeListener;
import itensil.repository.event.ContentEvent;
import itensil.util.UriHelper;

public class EntityContentListener implements ContentChangeListener {

	protected static Logger logger = Logger.getLogger(EntityContentListener.class);

	public void contentChanged(ContentEvent evt) {
		
		if (evt.getType() == ContentEvent.Type.REMOVE) return;

		try {
			MutableRepositoryNode recNode = (MutableRepositoryNode)
				((MutableRepositoryNode)evt.getNode()).getParent();

			EntityRecordIndexer recIdx = new EntityRecordIndexer(
					XMLDocument.readStream(RepositoryHelper.loadContent(
					UriHelper.absoluteUri(UriHelper.getParent(UriHelper.getParent(recNode.getUri())), "model.entity")
					)));
			
			NodeContent cont = evt.getContent();
			
			recIdx.indexRecord(recNode, cont.getStream());
			
		} catch (Exception ex) {
			logger.warn("Problem indexing entity data.", ex);
		}

	}

}
