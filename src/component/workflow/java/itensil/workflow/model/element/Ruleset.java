package itensil.workflow.model.element;

import itensil.workflow.model.BasicElement;
import itensil.workflow.model.ContainerElement;
import itensil.workflow.model.FlowModel;

public class Ruleset extends BasicElement {
	
	public final static String NAME = "ruleset";
	public final static String [] ATTRIBUTES = {"src", "rule"};
	
	
	protected Object ruleObj;

	public Ruleset(FlowModel owner, ContainerElement parent) {
        super(owner, parent);
    }
	
	public String[] getAttributeNames() {
		return Ruleset.ATTRIBUTES;
	}

	public String getElementName() {
		return Ruleset.NAME;
	}

	public Object getRuleObj() {
		return ruleObj;
	}

	public void setRuleObj(Object ruleObj) {
		this.ruleObj = ruleObj;
	}

	
}
