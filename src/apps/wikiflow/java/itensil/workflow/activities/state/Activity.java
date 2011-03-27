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
package itensil.workflow.activities.state;

import itensil.util.Check;
import itensil.workflow.state.Token;
import itensil.workflow.state.StepState;
import itensil.workflow.activities.state.ActivityRole;
import itensil.workflow.activities.state.FlowState;
import itensil.workflow.activities.state.ActivityPlan;
import itensil.workflow.activities.state.ActivityCurrentPlan;
import itensil.io.HibernateUtil;
import itensil.repository.RepositoryNode;
import itensil.repository.RepositoryHelper;
import itensil.repository.NotFoundException;
import itensil.repository.AccessDeniedException;

import java.util.*;
import java.io.Serializable;

import org.dom4j.Document;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * @author ggongaware@itensil.com
 *
 */
public class Activity extends Token implements Serializable {

    // Activity ID will match RepositoryNode Id for the item's folder

    private FlowState flow;
    private String name;
    private String description;
    private Activity parent;
    private String submitId;
    private String userSpaceId;
    private String variationId;
    private Date timeStamp;
    private int priority;
    private int duration;
    private Date startDate;
    private Date dueDate;
    private transient RepositoryNode node;
    private transient RepositoryNode variationNode;
    private transient Document dataCache;
    
    
    String cust0Val;
    String cust1Val;
    String cust2Val;
    String cust3Val;
    String cust4Val;
    String cust5Val;
    String cust6Val;
    String cust7Val;
    String cust8Val;
    String cust9Val;
    String custAVal;
    String custBVal;

    String contextGroupId;
    
    private Map<String, ActivityStepState> states = new HashMap<String, ActivityStepState>();
    private Map<String, ActivityRole> roles = new HashMap<String,ActivityRole>();
    private Map<String, ActivityPlan> plans = new HashMap<String,ActivityPlan>();
    private Set<Activity> children = new HashSet<Activity>();
    private Set<Activity> activeChildren = null;
    private Set<String> projects = new HashSet<String>();

    public Activity() {
        super(null);
    }

    public Activity(String id) {
        super(id);
    }

    public String getUserSpaceId() {
		return userSpaceId;
	}

	public void setUserSpaceId(String userSpaceId) {
		this.userSpaceId = userSpaceId;
	}

	public String getName() {
        return name;
    }

    public void setName(String val) {
    	if (val != null && val.length() > 250) {
    		this.name = val.substring(0,250) + "...";
    	} else {
    		this.name = val;
    	}
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String val) {
    	if (val != null && val.length() > 250) {
    		this.description = val.substring(0,250) + "...";
    	} else {
    		this.description = val;
    	}
    }

    public String getSubmitId() {
        return submitId;
    }

    public void setSubmitId(String val) {
        this.submitId = val;
    }

    public Map<String,ActivityRole> getRoles() {
        return roles;
    }

    public void setRoles(Map<String,ActivityRole> val) {
        this.roles = val;
    }

    public Map<String,ActivityPlan> getPlans() {
        return plans;
    }

    public void setPlans(Map<String,ActivityPlan> val) {
        this.plans = val;
    }

    public Date getTimeStamp() {
        return (timeStamp == null) ? null : new Date(timeStamp.getTime());
    }

    public void setTimeStamp(Date val) {
        this.timeStamp = (val == null) ? null : new Date(val.getTime());
    }

    public final Date getDueDate() {
        return (dueDate == null) ? null : new Date(dueDate.getTime());
    }

    public void setDueDate(Date val) {
        this.dueDate = (val == null) ? null : new Date(val.getTime());
    }

    public Set<Activity> getChildren() {
        return children;
    }

    public void setChildren(Set<Activity> val) {
        this.children = val;
    }

    public Activity getParent() {
        return parent;
    }

    public void setParent(Activity val) {
        this.parent = val;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public FlowState getFlow() {
        return flow;
    }

    public void setFlow(FlowState flow) {
        this.flow = flow;
    }

    public Map<String, ActivityStepState> getStates() {
        return states;
    }

    public void setStates(Map<String, ActivityStepState> states) {
        this.states = states;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

	public String getCust0Val() {
		return cust0Val;
	}

	public void setCust0Val(String cust0Val) {
		this.cust0Val = cust0Val;
	}

	public String getCust1Val() {
		return cust1Val;
	}

	public void setCust1Val(String cust1Val) {
		this.cust1Val = cust1Val;
	}

	public String getCust2Val() {
		return cust2Val;
	}

	public void setCust2Val(String cust2Val) {
		this.cust2Val = cust2Val;
	}

	public String getCust3Val() {
		return cust3Val;
	}

	public void setCust3Val(String cust3Val) {
		this.cust3Val = cust3Val;
	}

	public String getCust4Val() {
		return cust4Val;
	}

	public void setCust4Val(String cust4Val) {
		this.cust4Val = cust4Val;
	}

	public String getCust5Val() {
		return cust5Val;
	}

	public void setCust5Val(String cust5Val) {
		this.cust5Val = cust5Val;
	}

	public String getCust6Val() {
		return cust6Val;
	}

	public void setCust6Val(String cust6Val) {
		this.cust6Val = cust6Val;
	}

	public String getCust7Val() {
		return cust7Val;
	}

	public void setCust7Val(String cust7Val) {
		this.cust7Val = cust7Val;
	}

	public String getCust8Val() {
		return cust8Val;
	}

	public void setCust8Val(String cust8Val) {
		this.cust8Val = cust8Val;
	}

	public String getCust9Val() {
		return cust9Val;
	}

	public void setCust9Val(String cust9Val) {
		this.cust9Val = cust9Val;
	}

	public String getCustAVal() {
		return custAVal;
	}

	public void setCustAVal(String custAVal) {
		this.custAVal = custAVal;
	}

	public String getCustBVal() {
		return custBVal;
	}

	public void setCustBVal(String custBVal) {
		this.custBVal = custBVal;
	}

	public Set<String> getProjects() {
		return projects;
	}

	public void setProjects(Set<String> projects) {
		this.projects = projects;
	}

	public String getVariationId() {
		return variationId;
	}

	public void setVariationId(String variationId) {
		this.variationId = variationId;
		variationNode = null;
	}

	public RepositoryNode getVariationNode() throws NotFoundException, AccessDeniedException {
		if (variationNode == null) {
			String varId = getVariationId();
			variationNode = Check.isEmpty(varId) ? null : RepositoryHelper.getNodeById(getVariationId(), false);
        }
		return variationNode;
	}

	@SuppressWarnings("unchecked")
	public Set<Activity> getActiveChildren() {
    	if (activeChildren == null) {
    		Query qry = HibernateUtil.getSession().getNamedQuery("Activity.getActiveChildren");
    		qry.setEntity("parent", this);
    		activeChildren = new LinkedHashSet<Activity>(qry.list());
    	}
        return activeChildren;
    }


    public RepositoryNode getNode() throws NotFoundException, AccessDeniedException {
        if (node == null) {
            node = RepositoryHelper.getNodeById(getId(), false);
        }
        return node;
    }

    public void changeParent(Activity dstActivity) {
        Activity prev = getParent();
        if (prev != null) {
            Set<Activity> kids = prev.getActiveChildren();
            if (Hibernate.isInitialized(kids)) {
                kids.remove(this);
            }
        }
        setParent(dstActivity);
        if (dstActivity != null) {
        	Set<String> projs = this.getProjects();
        	for (String projId : dstActivity.getProjects()) {
        		projs.add(projId);
        	}
        	
            Set<Activity> kids = dstActivity.getActiveChildren();
            if (Hibernate.isInitialized(kids)) {
                kids.add(this);
            }
        }
    }
    
    public void clearCache() {
    	dataCache = null;
    }

    public Document getDataCache() {
		return dataCache;
	}

	public void setDataCache(Document dataCache) {
		this.dataCache = dataCache;
	}

	public String getContextGroupId() {
		return contextGroupId;
	}

	public void setContextGroupId(String contextGroupId) {
		this.contextGroupId = contextGroupId;
	}

	/**
     * 
     * @param id
     */
	public Activity changeActivityId(String id) {
		
		Session session = HibernateUtil.getSession();
		String qryNames[] = {
				"Activity.changeIdState",
				"Activity.changeIdPlan",
				"Activity.changeIdRole",
				"Activity.changeIdAlert",
				"Activity.changeIdTimer",
				"FlowState.changeIdLog",
				"Activity.changeId"
				};
		
		String oid = getId();
		for (String qryName : qryNames) {
			Query qry = session.getNamedQuery(qryName);
			qry.setString("oid", oid);
			qry.setString("nid", id);
			
			qry.executeUpdate();
		}
		session.evict(this);
		session.flush();
		
		return (Activity)session.get(Activity.class, id);
	}
}
