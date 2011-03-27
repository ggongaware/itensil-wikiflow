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
package itensil.workflow.track;

import itensil.web.MethodServlet;
import itensil.web.ServletUtil;
import itensil.web.ContentType;
import itensil.workflow.model.FlowSAXHandler;
import itensil.workflow.model.FlowModel;
import itensil.workflow.model.element.Condition;
import itensil.workflow.state.Token;
import itensil.workflow.state.SimulatorStateStore;
import itensil.workflow.Runner;
import itensil.workflow.FlowEvent;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Collection;

import org.dom4j.DocumentHelper;
import org.dom4j.Document;
import org.dom4j.Element;

/**
 * @author ggongaware@itensil.com
 *
 */
public class TrackingServlet extends MethodServlet {

    /**
     *  /simStart
     *
     * Start a flow simulator
     *
     * Takes an inputstream of an Flow XML Model
     *
     *  Output:
     *      Status XML
     *
     */
    @ContentType("text/xml")
    public void webSimStart(HttpServletRequest request, HttpServletResponse response) throws Exception {

        HttpSession session = request.getSession();

        // Init model
        FlowModel model = new FlowModel();
        FlowSAXHandler flowSax = new FlowSAXHandler(model);
        flowSax.parse(request.getInputStream());
        session.setAttribute("simModel", model);

        // Init Store & Eval
        SimulatorStateStore<Token,String> store = new SimulatorStateStore<Token,String>("simFlow", true, false);
        TrackingManualEval<Token,String> condEval = new TrackingManualEval<Token,String>();
        session.setAttribute("simStore", store);
        session.setAttribute("simCondEval", condEval);

        // Init Token
        Token tok = new Token("simTok0");
        session.setAttribute("simTk." + tok.getId(), tok);

        // inject token
        Runner<Token,String> run = getSimulatorRunner(request);
        run.handleEvent(new FlowEvent<Token,String>(tok, run.startToken(null, tok)));

        // send status
        TrackingXML<Token> track = new TrackingXML<Token>(store, store);
        sendTokenState(tok, track, condEval, response);
    }


    /**
     *  /simStatus
     *
     *  Send simulator status
     *
     *  Output:
     *      Status XML
     */
    @SuppressWarnings("unchecked")
	@ContentType("text/xml")
    public void webSimStatus(HttpServletRequest request, HttpServletResponse response) throws Exception {

        HttpSession session = request.getSession();
        SimulatorStateStore<Token,String> store = (SimulatorStateStore<Token,String>)session.getAttribute("simModel");

        TrackingXML<Token> track = new TrackingXML<Token>(store, store);
        Document doc = DocumentHelper.createDocument();
        track.appendAllActiveSteps(doc);

        ServletUtil.setExpired(response);
        doc.write(response.getWriter());
    }


    /**
     *  /simEvent
     *
     *  Send an event to the simulator
     *
     *  Parameters:
     *      step = step id
     *      token = token id
     *
     *  Output:
     *      Token Status XML + any conditional options
     */
    @ContentType("text/xml")
    public void webSimEvent(HttpServletRequest request, HttpServletResponse response) throws Exception {

        HttpSession session = request.getSession();
        Token tok = (Token)session.getAttribute("simTk." + request.getParameter("token"));
        String step = request.getParameter("step");
        Runner<Token,String> run = getSimulatorRunner(request);
        FlowEvent<Token,String> evt = new FlowEvent<Token,String>(tok, step);
        run.handleEvent(evt);
        SimulatorStateStore<Token,String> store = (SimulatorStateStore<Token,String>)run.getStates();
        TrackingXML<Token> track = new TrackingXML<Token>(store, store);
        sendTokenState(tok, track, (TrackingManualEval<Token,String>)run.getEvals(), response);
    }




    /**
     *  /simPick
     *
     *  Pick a condition for a flow switch
     *
     *  Parameters:
     *      step = step id
     *      token = token id
     *      return = condition return id(s)
     *
     *  Output:
     *      Token Status XML + any conditional options
     */
    @ContentType("text/xml")
    public void webSimPick(HttpServletRequest request, HttpServletResponse response) throws Exception {

        HttpSession session = request.getSession();
        Token tok = (Token)session.getAttribute("simTk." + request.getParameter("token"));
        String step = request.getParameter("step");
        Runner<Token,String> run = getSimulatorRunner(request);
        FlowEvent<Token,String> evt = new FlowEvent<Token,String>(tok, step);
        TrackingManualEval<Token,String> condEval = (TrackingManualEval<Token,String>)run.getEvals();
        condEval.clearPendingConditions(tok);
        run.handleEventCondition(evt, request.getParameterValues("return"));
        SimulatorStateStore<Token,String> store = (SimulatorStateStore<Token,String>)run.getStates();
        TrackingXML<Token> track = new TrackingXML<Token>(store, store);
        sendTokenState(tok, track, condEval, response);
    }

    /**
     * 500 Error
     */

    /**
     * Output:
     *      Token Status XML + any conditional options
     */
    protected void sendTokenState(
            Token tok,
            TrackingXML<Token> track,
            TrackingManualEval<Token,String> condEval,
            HttpServletResponse response)
            throws Exception {

        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("track");
        track.appendActiveSteps(tok, root);

        // send condinition options
        //   currently limited to single-state tokens, and switch mode=XOR
        Collection<Condition> conds = condEval.getPendingConditions(tok);
        if (conds != null) {
            Element tokElem = root.addElement("token-conditions");
            tokElem.addAttribute("id", tok.getId());
            for (Condition cond : conds) {
                Element ce = tokElem.addElement("condition");
                ce.addAttribute("returnId", cond.getReturnId());
                String str = cond.getInnerText();
                if (str != null) ce.addText(cond.getInnerText());
            }
        }

        ServletUtil.setExpired(response);
        //response.setContentType("text/xml");
        doc.write(response.getWriter());
    }

    @SuppressWarnings("unchecked")
	protected Runner<Token,String> getSimulatorRunner(HttpServletRequest request) {
        HttpSession session = request.getSession();
        SimulatorStateStore<Token,String> store = (SimulatorStateStore<Token,String>)session.getAttribute("simStore");
        FlowModel model = (FlowModel)session.getAttribute("simModel");
        TrackingManualEval<Token,String> condEval =
                (TrackingManualEval<Token,String>)session.getAttribute("simCondEval");
        return new Runner<Token,String>(model, store, store, condEval, null);
    }
}
