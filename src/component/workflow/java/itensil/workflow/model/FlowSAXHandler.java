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

import java.util.HashMap;

import itensil.io.xml.SAXHandler;
import itensil.util.Check;
import itensil.workflow.model.element.*;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author ggongaware@itensil.com
 *
 *
 *
 */
public class FlowSAXHandler extends SAXHandler {

    protected FlowModel model;
    protected BasicElement current;
    protected ContainerElement lastParent;
    protected StringBuffer textBuffer;
    protected HashMap<String, Class<? extends AppElement>> appTypes;


    public FlowSAXHandler(FlowModel model) {
        super();
        this.model = model;
        this.textBuffer = new StringBuffer();
        appTypes = new HashMap<String, Class<? extends AppElement>>();
    }
    
    /**
     * 
     * @param klass
     * @param namespaceURI
     * @param sName simple name/local name
     */
    public void addAppElementType(Class<? extends AppElement> klass, String namespaceURI, String sName) {
    	appTypes.put("{" + namespaceURI + "}" +  sName, klass);    	
    }

    public void startElement(String namespaceURI,
                             String sName, // simple name
                             String qName, // qualified name
                             Attributes attrs) throws SAXException {

        if (sName.length() < 1) {
            sName = qName;
        }
        textBuffer.setLength(0);

        ContainerElement parent = null;
        if (current instanceof ContainerElement) {
            parent = (ContainerElement)current;
        }
        // route this element
        if (!Check.isEmpty(namespaceURI) || current instanceof AppElement) {
        	AppElement appElem;
        	if (!appTypes.isEmpty()) {
        		Class<? extends AppElement> klass = appTypes.get("{" + namespaceURI + "}" +  sName);
        		if (klass != null) {
        			try {
						appElem = klass.newInstance();
					} catch (Exception e) {
						throw new SAXException("Error with app-type {" + namespaceURI + "}" +  sName, e);
					}
        		} else {
        			appElem = new AppElement();
        		}
        	} else {
        		appElem = new AppElement();
        	}
            appElem.init(sName, namespaceURI, model, current);
            int len = attrs.getLength();
            for (int ii = 0; ii < len; ii++) {
                appElem.setAttribute(attrs.getQName(ii), attrs.getValue(ii));
            }
            if (current == null) {
                model.addAppElement(appElem);
            } else if (current instanceof AppElement) {
            	((AppElement)current).addChild(appElem);
            }
            current = appElem;
        }
          // hopefully sorted by the element frequency
        else if (sName.equals(Label.NAME))          current = new Label(model, parent);
        else if (sName.equals(Path.NAME))           current = new Path(model, parent);
        else if (sName.equals(Condition.NAME))      current = new Condition(model, parent);
        else if (sName.equals(Description.NAME))    current = new Description(model, parent);
        else if (sName.equals(ActivityStep.NAME))   current = new ActivityStep(model, parent);
        else if (sName.equals(Timer.NAME))          current = new Timer(model, parent);
        else if (sName.equals(Switch.NAME))         current = new Switch(model, parent);
        else if (sName.equals(Ruleset.NAME))      	current = new Ruleset(model, parent);
        else if (sName.equals(Until.NAME))          current = new Until(model, parent);
        else if (sName.equals(Wait.NAME))           current = new Wait(model, parent);
        else if (sName.equals(Note.NAME))           current = new Note(model, parent);
        else if (sName.equals(End.NAME))            current = new End(model, parent);
        else if (sName.equals(Start.NAME))          current = new Start(model, parent);
        else if (sName.equals(Enter.NAME))          current = new Enter(model, parent);
        else if (sName.equals(Exit.NAME))           current = new Exit(model, parent);
        else if (sName.equals(Group.NAME))          current = new Group(model, parent);
        else if (sName.equals(Script.NAME))         current = new Script(model, parent);
        else 										current = null;

        if (current != null) {
        	if (!(current instanceof AppElement)) {
        		
	            if (current instanceof ContainerElement) {
	                ((ContainerElement)current).setId(attrs.getValue("id"));
	                if (current instanceof Step) {
	                    model.addStep((Step)current);
	                } else if (current instanceof Note) {
	                    model.addNote((Note)current);
	                }
	            }
	            
	            String attNames[] = current.getAttributeNames();
	            for (String attName : attNames) {
	                current.setAttribute(attName, attrs.getValue(attName));
	            }
            }
            if (parent != null) {
                parent.addChild(current);
            }
        }
    }

    public void endElement(String namespaceURI,
            String sName, // simple name
            String qName // qualified name
            ) throws SAXException {

        if (current != null) {
            if (textBuffer.length() > 0) {
                current.setInnerText(textBuffer.toString());
                textBuffer.setLength(0);
            }
            current = current.getParent();
        }
    }

    public void characters(char buf[], int offset, int len) throws SAXException {
        textBuffer.append(buf, offset, len);
    }


	public void endDocument() throws SAXException {
		model.setLoaded(true);
	}    

}
