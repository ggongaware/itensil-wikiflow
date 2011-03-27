package itensil.workflow.model.element;

import itensil.util.Check;
import itensil.workflow.model.BasicElement;
import itensil.workflow.model.ContainerElement;
import itensil.workflow.model.FlowModel;

public class Script extends BasicElement {

    public final static String NAME = "script";
    public final static String [] ATTRIBUTES = {"on"};
    
    public enum ON {enter, exit};

    public Script(FlowModel owner, ContainerElement parent) {
        super(owner, parent);
    }

    public String getElementName() {
        return NAME;
    }

    public String [] getAttributeNames() {
        return ATTRIBUTES;
    }
    
    public ON getOn() {
    	ON on = ON.enter;
    	String str = getAttribute("on");
    	if (!Check.isEmpty(str)) {
    		try {
    			on = ON.valueOf(str);
    		} catch (IllegalArgumentException iae) { }
    	}
    	return on;
    }

}
