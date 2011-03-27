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
package itensil.workflow.model;

import itensil.util.Check;
import itensil.workflow.model.element.Path;
import itensil.workflow.model.element.Start;
import itensil.workflow.model.element.Step;
import itensil.workflow.model.element.Note;

import org.dom4j.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author ggongaware@itensil.com
 *
 */
public class FlowModel implements Serializable {

    protected HashMap<String, Step> steps;
    protected HashMap<String, Note> notes;
    protected ArrayList<Start> startSteps;
    protected ArrayList<AppElement> appElements;
    protected long loadTime;
    protected boolean loaded;

    public FlowModel() {
        steps = new HashMap<String, Step>();
        notes = new HashMap<String, Note>();
        startSteps = new ArrayList<Start>();
        appElements = new ArrayList<AppElement>();
        loadTime = System.currentTimeMillis();
        loaded = false;
    }

    public Step getStep(String id) {
        return steps.get(id);
    }

    public void addStep(Step step) {
        steps.put(step.getId(), step);
        if (step instanceof Start) {
            startSteps.add((Start)step);
        }
    }

    public void addNote(Note note) {
        notes.put(note.getId(), note);
    }

    public Collection<Start> getStartSteps() {
        return startSteps;
    }

    public void addAppElement(AppElement elem) {
        appElements.add(elem);
    }

    public Collection<AppElement> matchAppElements(String namespaceUri, String name) {
        ArrayList<AppElement> matches = new ArrayList<AppElement>();
        for (AppElement elem : appElements) {
            if (elem.getNamespaceURI().equals(namespaceUri)) {
                if (name == null || elem.getElementName().equals(name))
                    matches.add(elem);
            }
        }
        return matches;
    }
    
    public <T extends AppElement> Collection<T> selectAppChildren(Class<T> klass) {
    	ArrayList<T> select = new ArrayList<T>();
    	for (AppElement kid : appElements) {
    		if (klass.isInstance(kid)) {
    			select.add(klass.cast(kid));
    		}
    	}
    	return select;
    }
    
    public <T extends AppElement> T selectOneAppChild(Class<T> klass) {
    	for (AppElement kid : appElements) {
    		if (klass.isInstance(kid)) return klass.cast(kid);
    	}
    	return null;
    }

    public Collection<AppElement> getAppElements() {
        return appElements;
    }

    public long getLoadTime() {
        return loadTime;
    }

    public void loadXML(InputStream in) throws IOException  {
    	loadXML(new FlowSAXHandler(this), in);
    }
    
    public void loadXML(FlowSAXHandler saxHand, InputStream in) throws IOException  {
    	try {
            loadTime = System.currentTimeMillis();
            saxHand.parse(in);
        } catch (SAXException saxe) {
            throw new IOException(saxe.getMessage());
        }
    }
    

	public Collection<Path> getFromPaths(String id) {
		ArrayList<Path> fpaths = new ArrayList<Path>();
		for (Step stp : steps.values()) {
			for (Path pth : stp.getPaths()) {
				if (id.equals(pth.getTo())) {
					fpaths.add(pth);
				}
			}
		}
		return fpaths;
	}
	
	public ValidationLogger validate() {
		ValidationLogger vl = new ValidationLogger();
		validate(vl);
		return vl;
	}
	
	public void validate(ValidationLogger vlogger) {
		
		// validate start
		if (startSteps.isEmpty()) {
			vlogger.error(null, "flow", "Missing Start step");
		}
		for (Step stp : steps.values()) {
			stp.validate(vlogger);
		}
		for (AppElement apEl : getAppElements()) {
			apEl.validate(vlogger);
		}
	}
	

	/**
	 * 
	 * @param stepSeqParent
	 * @param idMap
	 */
	public static void collectIdChanges(Element stepSeqParent,  Map<String, String> idMap, Collection<String> newIds) {
		Iterator itr = stepSeqParent.elementIterator();
		while (itr.hasNext()) {
			Element elm = (Element)itr.next();
			String oldid = elm.attributeValue("oldid");
			if (!Check.isEmpty(oldid)) {
				if ("//N".equals(oldid)) newIds.add(elm.attributeValue("id"));
				else idMap.put(oldid, elm.attributeValue("id"));
			}
			if ("group".equals(elm.getName())) {
				collectIdChanges(elm, idMap, newIds);
			}
		}
	}

	public boolean isLoaded() {
		return loaded;
	}

	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}

}
