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

import itensil.io.HibernateUtil;
import itensil.repository.MutableRepositoryNode;
import itensil.repository.NotFoundException;
import itensil.repository.AccessDeniedException;
import itensil.repository.RepositoryHelper;
import itensil.util.Check;
import itensil.workflow.activities.state.FlowRole;

import java.util.*;
import java.io.Serializable;

import org.hibernate.Query;
import org.hibernate.Session;

/**
 * @author ggongaware@itensil.com
 *
 */
public class FlowState implements Serializable {

    private String id; // ID will match RepositoryNode Id for file.flow
    private boolean active;
    private Map<String,FlowRole> roles = new HashMap<String, FlowRole>();
    private MutableRepositoryNode node;
    
    FlowColumn cust0;
    FlowColumn cust1;
    FlowColumn cust2;
    FlowColumn cust3;
    FlowColumn cust4;
    FlowColumn cust5;
    FlowColumn cust6;
    FlowColumn cust7;
    FlowColumn cust8;
    FlowColumn cust9;
    FlowColumn custA;
    FlowColumn custB;

    public FlowState() {}

    public FlowState(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Map<String,FlowRole> getRoles() {
        return roles;
    }

    public void setRoles(Map<String,FlowRole> roles) {
        this.roles = roles;
    }


	public FlowColumn getCust0() {
		return cust0;
	}

	public void setCust0(FlowColumn cust0) {
		this.cust0 = cust0;
	}

	public FlowColumn getCust1() {
		return cust1;
	}

	public void setCust1(FlowColumn cust1) {
		this.cust1 = cust1;
	}

	public FlowColumn getCust2() {
		return cust2;
	}

	public void setCust2(FlowColumn cust2) {
		this.cust2 = cust2;
	}

	public FlowColumn getCust3() {
		return cust3;
	}

	public void setCust3(FlowColumn cust3) {
		this.cust3 = cust3;
	}

	public FlowColumn getCust4() {
		return cust4;
	}

	public void setCust4(FlowColumn cust4) {
		this.cust4 = cust4;
	}

	public FlowColumn getCust5() {
		return cust5;
	}

	public void setCust5(FlowColumn cust5) {
		this.cust5 = cust5;
	}

	public FlowColumn getCust6() {
		return cust6;
	}

	public void setCust6(FlowColumn cust6) {
		this.cust6 = cust6;
	}

	public FlowColumn getCust7() {
		return cust7;
	}

	public void setCust7(FlowColumn cust7) {
		this.cust7 = cust7;
	}

	public FlowColumn getCust8() {
		return cust8;
	}

	public void setCust8(FlowColumn cust8) {
		this.cust8 = cust8;
	}

	public FlowColumn getCust9() {
		return cust9;
	}

	public void setCust9(FlowColumn cust9) {
		this.cust9 = cust9;
	}

	public FlowColumn getCustA() {
		return custA;
	}

	public void setCustA(FlowColumn custA) {
		this.custA = custA;
	}

	public FlowColumn getCustB() {
		return custB;
	}

	public void setCustB(FlowColumn custB) {
		this.custB = custB;
	}

	public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final FlowState flowState = (FlowState) o;

        return id.equals(flowState.id);
    }

    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Cache only
     */
    public void setNode(MutableRepositoryNode node) {
        this.node = node;
    }

    public MutableRepositoryNode getNode() throws NotFoundException, AccessDeniedException {
        if (node == null) {
            node = RepositoryHelper.getNodeById(getId(), false);
        }
        return node;
    }

    /**
     * 
     * @param flowActs
     * @param idMap
     */
	public void activateIdChanges(Collection<Activity> flowActs, Map<String, String> idMap) {
		
		Session session = HibernateUtil.getSession();
		for (Activity act : flowActs) {
			
			for (Map.Entry<String, String> ids : idMap.entrySet()) {
				ActivityStepState stat = act.getStates().remove(ids.getKey());
				if (stat != null) {
					stat.setStepId(ids.getValue());
					act.getStates().put(ids.getValue(), stat);
					session.update(stat);
				}
				ActivityPlan plan = act.getPlans().remove(ids.getKey());
				if (plan != null) {
					plan.setStepId(ids.getValue());
					act.getPlans().put(ids.getValue(), plan);
					session.update(plan);
				}
			}
		}
	}
	
	/**
	 * 
	 * @param flowActs
	 * @param id
	 * @param type
	 */
	public void removeActiveId(Collection<Activity> flowActs, String id, String type) {
		Session session = HibernateUtil.getSession();
		for (Activity act : flowActs) {
			ArrayList<String> ids = new ArrayList<String>();
			if ("group".equals(type)) {
				String gid = id + "/";
				for (String sid : act.getStates().keySet()) {
					if (sid.startsWith(gid)) {
						ids.add(sid);
					}
				}
				for (String sid : act.getPlans().keySet()) {
					if (sid.startsWith(gid)) {
						ids.add(sid);
					}
				}
			} else {
				ids.add(id);
			}
			for (String rid : ids) {
				ActivityStepState stat = act.getStates().remove(rid);
				if (stat != null) session.delete(stat);
				
				ActivityPlan plan = act.getPlans().remove(rid);
				if (plan != null) session.delete(plan);
			}
		}
		
	}
	
	
}
