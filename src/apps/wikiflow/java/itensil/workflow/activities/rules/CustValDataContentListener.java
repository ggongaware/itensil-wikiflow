package itensil.workflow.activities.rules;

import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.hibernate.Session;

import itensil.io.HibernateUtil;
import itensil.io.xml.XMLDocument;
import itensil.repository.NodeContent;
import itensil.repository.event.ContentChangeListener;
import itensil.repository.event.ContentEvent;
import itensil.util.Check;
import itensil.workflow.activities.state.Activity;
import itensil.workflow.activities.state.FlowColumn;
import itensil.workflow.activities.state.FlowState;
import itensil.workflow.model.AppElement;
import itensil.workflow.model.FlowModel;

public class CustValDataContentListener implements ContentChangeListener {

	protected static Logger logger = Logger.getLogger(CustValDataContentListener.class);
	
	/**
	 * Note: Assumes an active transaction
	 */
	public void contentChanged(ContentEvent evt) {
		
		if (evt.getType() == ContentEvent.Type.REMOVE) return;
		
		/*
		 * Check if the file is a child of an activity
		 */
		Session session = HibernateUtil.getSession();
		Activity act = (Activity)session.get(Activity.class, evt.getNode().getParentNodeId());
		if (act == null) return;
		
		/*
		 * Parse the file
		 */
		NodeContent cont = evt.getContent();
		Document doc;
		try {
			doc = XMLDocument.readStream(cont.getStream());
		} catch (DocumentException ex) {
			logger.warn("Problem parsing acitivity data.", ex);
			return;
		}
		
		/*
		 * Line-up the cust val columns with first level xml elements
		 */
		Element root = doc.getRootElement();
		FlowState flow = act.getFlow();
		act.setCust0Val(columnValue(flow.getCust0(), root));
		act.setCust1Val(columnValue(flow.getCust1(), root));
		act.setCust2Val(columnValue(flow.getCust2(), root));
		act.setCust3Val(columnValue(flow.getCust3(), root));
		act.setCust4Val(columnValue(flow.getCust4(), root));
		act.setCust5Val(columnValue(flow.getCust5(), root));
		act.setCust6Val(columnValue(flow.getCust6(), root));
		act.setCust7Val(columnValue(flow.getCust7(), root));
		act.setCust8Val(columnValue(flow.getCust8(), root));
		act.setCust9Val(columnValue(flow.getCust9(), root));
		act.setCustAVal(columnValue(flow.getCustA(), root));
		act.setCustBVal(columnValue(flow.getCustB(), root));
		
		session.saveOrUpdate(act);
	}
	
	/**
	 * Note: Does not call save
	 */
	public static void modelStateSync(FlowState flow, FlowModel flowMod) {
		 Collection<AppElement> elems = flowMod.matchAppElements("http://itensil.com/workflow", "data");
		 if (elems.isEmpty()) return;
		 AppElement dataEl = elems.iterator().next();
		 Collection<AppElement> attrElems = dataEl.matchChildElements("attr");
		 Iterator<AppElement> itr = attrElems.iterator();
		 flow.setCust0(attr2Column(itr.hasNext() ? itr.next() : null, flow));
		 flow.setCust1(attr2Column(itr.hasNext() ? itr.next() : null, flow));
		 flow.setCust2(attr2Column(itr.hasNext() ? itr.next() : null, flow));
		 flow.setCust3(attr2Column(itr.hasNext() ? itr.next() : null, flow));
		 flow.setCust4(attr2Column(itr.hasNext() ? itr.next() : null, flow));
		 flow.setCust5(attr2Column(itr.hasNext() ? itr.next() : null, flow));
		 flow.setCust6(attr2Column(itr.hasNext() ? itr.next() : null, flow));
		 flow.setCust7(attr2Column(itr.hasNext() ? itr.next() : null, flow));
		 flow.setCust8(attr2Column(itr.hasNext() ? itr.next() : null, flow));
		 flow.setCust9(attr2Column(itr.hasNext() ? itr.next() : null, flow));
		 flow.setCustA(attr2Column(itr.hasNext() ? itr.next() : null, flow));
		 flow.setCustB(attr2Column(itr.hasNext() ? itr.next() : null, flow));
	}
	
	private static FlowColumn attr2Column(AppElement attrElem, FlowState flow) {
		
		if (attrElem == null) return null;
		String name = attrElem.getAttribute("name");
		if (Check.isEmpty(name)) return null;
		String type = attrElem.getAttribute("type");
		
		FlowColumn col = new FlowColumn();
		col.setName(name);
		col.setType(Check.isEmpty(type) ? "xsd:string" : type);
		col.setFlow(flow);
		return col;
	}
	
	private static String columnValue(FlowColumn col, Element parent) {
		if (col == null) return null;
		String txt = parent.elementText(col.getName());
		if (!Check.isEmpty(txt) && txt.length() > 255) {
			txt = txt.substring(0, 250) + "...";
		}
		return txt;
	}

}
