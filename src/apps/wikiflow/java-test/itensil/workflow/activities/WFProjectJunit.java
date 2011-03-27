package itensil.workflow.activities;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import itensil.io.HibernateUtil;
import itensil.workflow.activities.state.Activity;
import itensil.workflow.activities.state.ActivityStepState;
import itensil.workflow.activities.state.FlowState;
import junit.framework.TestCase;

public class WFProjectJunit extends TestCase {
	
	@SuppressWarnings("unchecked")
	public void testProjectLoad1() throws Exception {

        HibernateUtil.beginTransaction();
        Object del = HibernateUtil.getSession().get(FlowState.class, "AAAAAAAAAAAAAAAAAAAB");
        if (del != null) HibernateUtil.getSession().delete(del);
        del = HibernateUtil.getSession().get(Activity.class, "AAAAAAAAAAAAAAAAAAAC");
        if (del != null) HibernateUtil.getSession().delete(del);
        del = HibernateUtil.getSession().get(Activity.class, "AAAAAAAAAAAAAAAAAAAF");
        if (del != null) HibernateUtil.getSession().delete(del);
        HibernateUtil.commitTransaction();
        
        HibernateUtil.beginTransaction();
    	Query qry = HibernateUtil.getSession().getNamedQuery("FlowState.getProjectFlows");
    	qry.setString("projId", "AAAAAAAAAAAAAAAAAAAA");
    	List<FlowState> flows = qry.list();
    	assertEquals(0, flows.size());
    	
    	FlowState flow = new FlowState("AAAAAAAAAAAAAAAAAAAB");
    	HibernateUtil.getSession().persist(flow);
    	
    	Activity act1 = new Activity("AAAAAAAAAAAAAAAAAAAC");
    	act1.setSubmitId("AAAAAAAAAAAAAAAAAAAD");
    	act1.setUserSpaceId("AAAAAAAAAAAAAAAAAAAE");
    	act1.setFlow(flow);
    	act1.getProjects().add("AAAAAAAAAAAAAAAAAAAA");
    	act1.getProjects().add("AAAAAAAAAAAAAAAAAAAG");
    	
    	HibernateUtil.getSession().persist(act1);
    	
    	
    	Activity act2 = new Activity("AAAAAAAAAAAAAAAAAAAF");
    	act2.setSubmitId("AAAAAAAAAAAAAAAAAAAD");
    	act2.setUserSpaceId("AAAAAAAAAAAAAAAAAAAE");
    	act2.setFlow(flow);
    	
    	HibernateUtil.getSession().persist(act2);
    	
    	ActivityStepState state = new ActivityStepState();
    	state.setActivity(act1);
    	state.setStepId("state1");
    	act1.getStates().put("state1", state);
    	HibernateUtil.getSession().saveOrUpdate(state);

    	state = new ActivityStepState();
    	state.setActivity(act1);
    	state.setStepId("state2");
    	act1.getStates().put("state2", state);
    	HibernateUtil.getSession().saveOrUpdate(state);
    	
    	state = new ActivityStepState();
    	state.setActivity(act2);
    	state.setStepId("state3");
    	act2.getStates().put("state3", state);
    	HibernateUtil.getSession().saveOrUpdate(state);  	
    	
    	
    	HibernateUtil.commitTransaction();
    	
    	
    	HibernateUtil.beginTransaction();
    	
    	qry = HibernateUtil.getSession().getNamedQuery("FlowState.getProjectFlows");
    	qry.setString("projId", "AAAAAAAAAAAAAAAAAAAA");
    	flows = qry.list();
    	assertEquals(1, flows.size());
    	
    	assertEquals("AAAAAAAAAAAAAAAAAAAB", flows.get(0).getId());
    	
    	qry = HibernateUtil.getSession().getNamedQuery("Activity.getProjectActivities");
    	qry.setString("projId", "AAAAAAAAAAAAAAAAAAAA");
    	qry.setFirstResult(0); // needed for mind scrambling hibernate bug
    	List<Activity> acts = qry.list();
    	assertEquals(1, acts.size());
    	assertEquals("AAAAAAAAAAAAAAAAAAAC", acts.get(0).getId());
    	HibernateUtil.commitTransaction();
    	
    	HibernateUtil.beginTransaction();
    	HibernateUtil.getSession().refresh(act1);
    	assertEquals(2, act1.getProjects().size());
    	qry = HibernateUtil.getSession().getNamedQuery("Activity.getProjectActivities");
    	qry.setString("projId", "AAAAAAAAAAAAAAAAAAAA");
    	qry.setFirstResult(0); // needed for mind scrambling hibernate bug
    	acts = qry.list();
    	for (Activity delAct : acts) {
    		delAct.getProjects().remove("AAAAAAAAAAAAAAAAAAAA");
    	}
    	HibernateUtil.commitTransaction();
    	
    	HibernateUtil.beginTransaction();
    	HibernateUtil.getSession().refresh(act1);
    	assertEquals(1, act1.getProjects().size());
    	
    	HibernateUtil.commitTransaction();
    	
    	HibernateUtil.beginTransaction();
    	HibernateUtil.getSession().delete(act1);
    	HibernateUtil.getSession().delete(act2);
    	HibernateUtil.getSession().delete(flow);
    	
    	HibernateUtil.commitTransaction();
	 }
	
	
	public void testProjectsAssigned() throws Exception {
		Query qry = HibernateUtil.getSession().getNamedQuery("Activity.getAssignedProjects");
		qry.setString("userId", "AAAAAAAAAAAAAAAAAAAA");
		qry.setString("userSpaceId", "AAAAAAAAAAAAAAAAAAAB");
		qry.list();
	}
	
	public void testActiveProjectActivities() throws Exception {
		Query qry = HibernateUtil.getSession().getNamedQuery("Activity.activeProjectActivities");
		qry.setString("projId", "AAAAAAAAAAAAAAAAAAAA");
		qry.list();
	}
}
