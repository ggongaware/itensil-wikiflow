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
package itensil.workflow.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import itensil.workflow.FlowEvent;
import itensil.workflow.model.element.ActivityStep;
import itensil.workflow.model.element.Path;
import itensil.workflow.model.element.Start;
import itensil.workflow.model.element.Step;
import itensil.workflow.model.element.Switch;
import itensil.workflow.state.StateException;
import itensil.workflow.state.StateStore;
import itensil.workflow.state.StepState;
import itensil.workflow.state.SubState;
import itensil.workflow.state.Token;

/**
 * Traffic director for multi-state tokens
 * 
 * @author grantg
 *
 */
public class DefaultMultiTrafficDirector<Tk extends Token, Dt> implements TrafficDirector<Tk, Dt> {

	public Path[] travelPaths(Step step, FlowEvent<Tk, Dt> evt, StateStore<Tk> states) throws StateException {
		
		Collection<Path> paths = step.getPaths();
		
		/* TODO - research changing this in the future
		 * Check if you've been here before
		 */
		if (false) {
			
			/*
			 * am I in a sub loop? 
			 */
			boolean subLoop = false;
			
			if (subLoop) {
				// TODO
				return paths.toArray(new Path[paths.size()]);
			}
			/*
			 * not a sub loop 
			 */
			else {
				// return all paths
				return paths.toArray(new Path[paths.size()]);
			}
		}
		/*
		 * first time
		 */
		else {
			// return all paths
			return paths.toArray(new Path[paths.size()]);
		}
		
	}

	public boolean waitRequired(Step step, FlowEvent<Tk, Dt> evt, StateStore<Tk> states) throws StateException {
		return !(step instanceof Start) && !waitingForSteps(step, evt, states).isEmpty();
	}
	
	/**
	 * 
	 * @param step
	 * @param evt
	 * @param states
	 * @return
	 * @throws StateException 
	 */
	public Collection<Step> waitingForSteps(Step step, FlowEvent<Tk, Dt> evt, StateStore<Tk> states) throws StateException {
		Collection<Path> fromPaths = step.getOwner().getFromPaths(step.getId());
		ArrayList<Step> waitSteps = new ArrayList<Step>();
		
		/*
		 * Does this have more than one inbound path?
		 */
		if (fromPaths.size() > 1) {
			
			ArrayList<Step> doneSteps = new ArrayList<Step>();
			
			for (Path fp : fromPaths) {
				
				Step fs = fp.getFromStep();
				
				/*
				 * Is this step downstream?
				 */
				if (!isSuccessor(step, fs)) {
					
					/*
					 * if the from step is already in the history, you're not waiting for it
					 */
					if (!fs.getId().equals(evt.getStepId()) 
							&& states.getExitLogSteps(evt.getToken(), fs.getId()).isEmpty()) {
						waitSteps.add(fs);
					} else {
						doneSteps.add(fs);
					}
				}
			}
			
			Set<Step> actSteps = null;
			
			/*
			 * Qualify the dones
			 */
			if (!doneSteps.isEmpty()) {
				
				ArrayList<Step> qualifiedDones = new ArrayList<Step>(doneSteps.size());
				
				for (Step ds : doneSteps) {
					/*
					 * If one of your doneSteps is a Switch, check if an active token is in
					 * a preceding step, if so the Switch will get re-evaluated, 
					 * so move it to the waitSteps.
					 */
					boolean qualified = true;
					
					if (ds instanceof Switch) {

						if (actSteps == null) actSteps = getActiveSteps(step, evt, states);
						
						for (Step acSt : actSteps) {
							if (isSuccessor(acSt, ds)) {
								qualified = false;
								break;
							}
						}
					}	
					
					if (qualified) {
						qualifiedDones.add(ds);
					} else {
						waitSteps.add(ds);
					}
				}
				
				doneSteps = qualifiedDones;
			}
			
			
			/*
			 * Test for common preceding exited switches that are forks
			 */
			if (!waitSteps.isEmpty() && !doneSteps.isEmpty()) {
				
				ArrayList<Step> qualifiedWaits = new ArrayList<Step>(waitSteps.size());
				
				for (Step ws : waitSteps) {
					boolean qualified = true;
					
					Set<Switch> wsPreSw_unqual = getPrecedingSwitches(ws);
					HashSet<Switch> wsPreSw = new HashSet<Switch>();
					
					/*
					 * Throw out looped switches with preceding activesteps
					 * because looped can change their mind next iteration
					 */
					loopedSw : for (Switch psw : wsPreSw_unqual) {
						
						if (inLoop(psw)) {
							if (actSteps == null) actSteps = getActiveSteps(step, evt, states);
							
							for (Step acSt : actSteps) {
								if (isSuccessor(acSt, psw)) {
									continue loopedSw; // precesing active, skip adding
								}
							}
						}
						wsPreSw.add(psw);
					}
					
					dsFor : for (Step ds : doneSteps) {
						Set<Switch> dsPreSw = getPrecedingSwitches(ds);
						for (Switch psw : dsPreSw) {
							if (wsPreSw.contains(psw) && areAlternates(psw,ds,ws)) {
								qualified = false;
								break dsFor;
							}
						}
					}
					
					if (qualified) qualifiedWaits.add(ws);
				}
				
				waitSteps = qualifiedWaits;
			}
			
		}
		return waitSteps;
	}
	
	/**
	 * Method isAbsoluteSuccessor.
	 *
	 * Only reachable by continuing
	 *
	 * @param fromStep
	 * @param testStep
	 * @return boolean
	 * @throws StateException 
	 */
	public boolean isAbsoluteSuccessor(Step fromStep, Step testStep) {

		return !deepTraceToStart(testStep, fromStep, new HashSet<Step>())
				&& deepIsSuccessor(fromStep, testStep, new HashSet<Step>());
	}

	
	public Set<Step> getActiveSteps(Step step, FlowEvent<Tk, Dt> evt, StateStore<Tk> states) throws StateException {
		HashSet<Step> actSteps = new HashSet<Step>();
		for (StepState<Tk> actStepSt : states.getActiveSteps(evt.getToken())) {
			if (actStepSt.getSubState() != SubState.WAIT_ENTER_STEP) {
				Step acSt = step.getOwner().getStep(actStepSt.getStepId());
				if (acSt != null) actSteps.add(acSt);
			}
		}
		return actSteps;
	}
	
	/**
	 * reachable by continuing
	 */
	public boolean isSuccessor(Step fromStep, Step testStep) {
		return deepIsSuccessor(fromStep, testStep, new HashSet<Step>());
	}
	
	private boolean deepIsSuccessor(Step fromStep, Step testStep, Set<Step> visited) {
		if (fromStep == null || testStep == null)  {
			return false;
		} else if (fromStep == testStep) {
			return true;
		} else {
			for (Path fp : fromStep.getPaths()) {
				Step ts = fp.getToStep();
				if (ts == null) {
					return false;
				} else if (!visited.contains(ts)) {
					visited.add(ts);
					if (deepIsSuccessor(ts, testStep, visited)) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Are stepA and stepB alternate results of the testSwitch?
	 *
	 * For valid test, stepA and stepB should be pre-qualified as succesors to testSwitch
	 * 
	 * @param testSwitch
	 * @param stepA 
	 * @param stepB
	 * @return
	 */
	public boolean areAlternates(Switch testSwitch, Step stepA, Step stepB) {
		for (Path fp : testSwitch.getPaths()) {
			Step ts = fp.getToStep();
			/*
			 * Do both stepA & B pass through a common, non-loop, successor step
			 */
			if (!isSuccessor(ts, testSwitch)
					&& isSuccessor(ts, stepA)
					&& isSuccessor(ts, stepB)) {
				return false;
			}
		}
		return true;
	}
	
	
	/**
	 * Method deepTraceToStart.
	 *
	 * Trace to start, but abort each time you pass through the avoidStep
	 *
	 * @param testStep
	 * @param avoidStep
	 * @param visited
	 * @return boolean
	 */
	private boolean deepTraceToStart(Step testStep, Step avoidStep, Set<Step> visited) {
		if (testStep == null) {
			return true;
		} else if (testStep instanceof Start) {
			return true;
		} else if (testStep == avoidStep) {
			return false;
		}
		for (Path fp : testStep.getOwner().getFromPaths(testStep.getId())) {
			Step fs = fp.getFromStep();
			if (fs == null) return true;
			if (!visited.contains(fs)) {
				visited.add(fs);
				if (deepTraceToStart(fs, avoidStep, visited)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Does this step appear in a loop?
	 * 
	 * @param testStep
	 * @return
	 */
	public boolean inLoop(Step testStep) {
		Set<Step> visited = new HashSet<Step>();
		for (Path fp : testStep.getOwner().getFromPaths(testStep.getId())) {
			Step fs = fp.getFromStep();
			if (deepIsSuccessor(testStep, fs, visited)) return true;
		}
		return false;
	}
	
	/**
	 * includes testStep is testStep is a Switch
	 * @param testStep
	 * @return
	 */
	public Set<Switch> getPrecedingSwitches(Step testStep) {
		HashSet<Switch> preSw = new HashSet<Switch>();
		Set<Step> visited = new HashSet<Step>();
		visited.add(testStep);
		deepGetPrecedingSwitches(testStep, preSw, visited);
		return preSw;
	}
	
	private void deepGetPrecedingSwitches(Step testStep, Set<Switch> preSw, Set<Step> visited) {
		if (testStep instanceof Start) {
			return;
		}
		if (testStep instanceof Switch) {
			preSw.add((Switch)testStep);
		}
		for (Path fp : testStep.getOwner().getFromPaths(testStep.getId())) {
			Step fs = fp.getFromStep();
			if (!visited.contains(fs)) {
				visited.add(fs);
				deepGetPrecedingSwitches(fs, preSw, visited);
			}
		}
	}
	
	
}
