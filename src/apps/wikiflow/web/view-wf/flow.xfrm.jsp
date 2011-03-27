<%@ page import="itensil.security.User" contentType="text/xml" %>
<% 	User user = (User)request.getUserPrincipal();
	boolean useRuleset = false;
	boolean useEntities = false;
	boolean useOrgs = false;
	if (user != null) {
		useRuleset = user.getUserSpace().getFeatures().contains("rules");
		useEntities = user.getUserSpace().getFeatures().contains("entity");
		useOrgs = user.getUserSpace().getFeatures().contains("orgs");
	}
%>
<form xmlns="http://www.w3.org/2002/06/xhtml2"
    xmlns:xf="http://www.w3.org/2002/xforms"
    xmlns:exf="http://www.exforms.org/exf/1-0"
    xmlns:ev="http://www.w3.org/2001/xml-events"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
    xmlns:ix="http://itensil.com/ns/xforms"
    xmlns:iw="http://itensil.com/workflow"
    xmlns:rl="http://itensil.com/ns/rules">

    <xf:model id="fmodel">
        <xf:instance id="ins" src="" />

        <xf:instance id="pal">
        <data xmlns="" xmlns:iw="http://itensil.com/workflow">
			<activity oldid="//N" role="" flow=""><label/><description/>
            	<iw:article layout="2colA">Default workzone.
++++
[submit[!Continue]] 
</iw:article></activity>
			<switch oldid="//N" mode="XOR"><ruleset src="" rule=""/><label/><description/></switch>
			<timer oldid="//N" mode="wait"><wait days="" hours="" minutes=""/><label/><description/></timer>
			<start oldid="//N"><label/><description/></start>
			<end oldid="//N"><label/><description/></end>

           	<group id="Group" oldid="//N"><label /><description />
				<enter id="Group/$$S" oldid="//N" style="left:45px;top:145px"><label>Enter</label><description />
					<path id="gp" startDir="E" endDir="W" points="95,170-201,170" to="Group/$$E"><label /></path>
				</enter>
				<exit id="Group/$$E" oldid="//N" style="left:200px;top:145px"><label>Exit</label><description /></exit>
			</group>

            <path><label/></path>
			<line><label/></line>
            <note>New Note</note>
            <condition/>
            <description/>

            <timer-modes>
                <wait days="" hours="" minutes="" rev="1"/>
                <until type="daily" at="" rev="1"/>
                <until type="weekly" on="mon" at="" rev="1"/>
                <until type="monthly" ord="alpha" alpha="first" number="1" unit="day" on="wday" at="" rev="1"/>
                <until type="condition" rev="1"><condition/></until>
            </timer-modes>
               
          	<bundle id="branch">
                <activity id="Branch Choose" oldid="//N" role=""  style="left:0px;top:50px" fixedtype="1"><label/><description/>
                	<path id="bpA" to="Branch Switch" startDir="E" endDir="W" points="110,80-135,80" />
                    <iw:article layout="2colA">Default branch workzone.
++++
[submit[!Option 1|pick=1]] [submit[Option 2|pick=2]] 
</iw:article>
				</activity>
				<switch mode="XOR" id="Branch Switch" oldid="//N" style="left:135px;top:40px"><ruleset src="" rule=""/><label/><description/>
					<path to="Branch One" id="bpB" startDir="E" endDir="W" points="255,80-265,80-265,30-315,30">
						<label style="left:275px;top:45px">Pick 1</label><condition>pick=1</condition></path>
					<path to="Branch Two" id="bpC" startDir="E" endDir="W" points="255,80-265,80-265,135-315,135">
						<label style="left:275px;top:110px">Pick 2</label><condition>pick=2</condition></path>
				</switch>
				<activity id="Branch One" oldid="//N" role="" style="left:315px;top:0px"><label/><description/>
                   <iw:article layout="2colA">Default workzone.
++++
[submit[!Continue]] 
</iw:article>
				</activity>
				<activity id="Branch Two" oldid="//N" role="" style="left:315px;top:105px"><label/><description/>
                   <iw:article layout="2colA">Default workzone.
++++
[submit[!Continue]] 
</iw:article>
				</activity>
           	</bundle>
			<bundle id="loop">
				<activity role="" oldid="//N" id="Loop Choose" style="left:0px;top:15px" fixedtype="1"><label/><description/>
					<iw:article layout="2colA">Default loop workzone.
++++
[submit[!Loop|loop=1]] [submit[No Loop|loop=0]] 
</iw:article>
					<path id="lpA" startDir="E" endDir="W" points="110,40-135,40" to="Loop Switch"><label/></path>
				</activity>
				<switch mode="XOR" oldid="//N" id="Loop Switch" style="left:135px;top:0px"><ruleset src="" rule=""/><label/><description/>
					<path id="lpC" startDir="S" endDir="S" points="195,80-195,110-50,110-50,75" to="Loop Choose">
						<condition>loop = 1</condition>
						<label style="left:120px;top:95px">Loop</label>
					</path>
					<path id="lpB" startDir="E" endDir="W" points="255,40-315,40" to="Loop End">
						<condition>loop = 0</condition>
						<label style="left:265px;top:50px">No Loop</label>
					</path>
				</switch>
				<activity role="" oldid="//N" id="Loop End" style="left:315px;top:15px"><label/><description/>
					<iw:article layout="2colA">Default workzone.
++++
[submit[!Continue]] 
</iw:article>
				</activity>
			</bundle>
			<bundle id="approval-loop">
				<activity role="Reviewer" oldid="//N" id="Review" style="left:25px;top:85px" fixedtype="1"><label/><description/>
					<iw:article layout="2colA">Default workzone.
++++
[submit[!Revise|loop=1]] [submit[!Approve|loop=0]]  
</iw:article>
					<path id="path 2" startDir="S" endDir="N" points="80,145-80,165" to="Approval Switch"><label/></path>
				</activity>
				<switch mode="XOR" oldid="//N" id="Approval Switch" style="left:20px;top:165px"><ruleset src="" rule=""/><label/><description/>	
					<path id="lpC" startDir="W" endDir="W" points="20,205-0,205-0,50-25,50" to="">
						<condition>loop = 1</condition>
						<label style="left:0px;top:215px">Revise</label>
					</path>
					<path id="lpB" startDir="E" endDir="W" points="140,205-195,205-195,65-195,65-195,55" to="">
						<condition>loop = 0</condition>
						<label style="left:135px;top:215px">Approve</label>
					</path>
				</switch>
			</bundle>
			<iw:attr name="" type="xsd:string"/>
			<iw:entity name="" type="" relation="1orMore" condition="newOrExisting"/>
			<item label="Label" value="value"/>
			<script on="enter"></script>
			<member/>
        </data>
        </xf:instance>
        
        <xf:instance id="meet">
       	<meet xmlns="">
    		<activity/>
	     	<mail>0</mail>
	     	<meetstart/>
	     	<meetend/>
	     	<body/>
    	</meet>
        </xf:instance>

		<% if (useEntities) { %>
        <xf:instance id="entIns" src="../entity/listEntities?uri=/home/entity"/>
     	<% } %>
     	
     	<% if (useOrgs) { %>
        <xf:instance id="opos" src="../fil/home/org-positions.xml">
        	<org-positions xmlns=""/>
        </xf:instance>
        <% } %>

        <xf:bind nodeset="steps//*/@id" type="ix:uniquePath" constraint="normalize-space(.) != ''"/>
        <xf:bind nodeset="steps/timer/wait/@*" type="xsd:int"/>
        <xf:bind nodeset="steps/timer/until/@at" type="xsd:time"/>
        <xf:bind nodeset="instance('meet')/meetstart|instance('meet')/meetend" type="xsd:dateTime"/>
        <xf:bind nodeset="iw:data/iw:attr/@name" type="ix:varName"/>
        <xf:bind nodeset="iw:data/iw:entity/@name" type="ix:varName"/>
        <xf:bind nodeset="steps//switch/ruleset/@src" type="ix:file"/>

        <!--
        <xf:action ev:event="xforms-model-construct-done" exf:if="not(description)">
            <xf:duplicate ref="." origin="instance('pal')/description"/>
        </xf:action> -->

        <xf:submission id="submission" replace="none" method="post" action="">
            <script ev:event="xforms-submit-done">
            	window.setTimeout('Ephemeral.topMessage(null, "Changes Saved.")', 10);
			</script>
			<xf:action ev:event="xforms-submit-error">
				<xf:message level="modal">Problem saving...</xf:message>
			</xf:action>
		</xf:submission>
		
		<xf:submission id="submit-draft" instance="meet" replace="none" method="post" action="../mod/saveMeetDraft">
			<xf:action ev:event="xforms-submit">
				<script>
		    		ProcOutline.initDraft(model.context, model.getInstanceDocument('meet').documentElement, model);
		    	</script>
			</xf:action>
			<xf:action ev:event="xforms-submit-done">
                <xf:dispatch name="xforms-close" target="save_meet_diag"/>
                <xf:message level="ephemeral">App saved.</xf:message>
			</xf:action>
			<xf:action ev:event="xforms-submit-error">
				<xf:message level="modal">Problem saving draft...</xf:message>
			</xf:action>
		</xf:submission>
    </xf:model>

    <xf:trigger>
    	<script ev:event="DOMActivate">
    		model.context.beginSave();
    	</script>
        <xf:label><span class="mbIco mb_savIco">&#160;</span>Save</xf:label>
    </xf:trigger>
    <div id="canvasPath" style="position:absolute;left:80px;top:3px;"/>
    <!-- <a href="javascript://"><script ev:event="click">
    	console.dirxml(model.getDefaultInstance());
    </script>s</a> -->


	<ix:template name="save">
		<div style="padding:2px 8px 10px 8px;width:220px">
		<div style="border:1px solid #ccc;padding:4px;margin:6px 2px 2px 2px;font-size:12px">Save to the default process.</div>
		<xf:trigger class="diagBtn dbSaveDef">
	    	<xf:action ev:event="DOMActivate">
		    	<script>
		    		model.context.commitSave();
		    	</script>
		    	<xf:dispatch name="xforms-close"/>
		    </xf:action>

	    </xf:trigger>
	    
	    <div style="border:1px solid #ccc;padding:4px;margin:20px 2px 2px 2px;font-size:12px">
			Save a variation just for the activity:<br/><xf:output value="$activityName" style="font-weight:bold"/>.
		</div>
	    <xf:trigger class="diagBtn dbSaveAct">
	    	<xf:action ev:event="DOMActivate">
		    	<script>
		    		model.context.commitVariationSave();
		    	</script>
		    	<xf:dispatch name="xforms-close"/>
		    </xf:action>
	    </xf:trigger>
		</div>
	</ix:template>
	
	<ix:template name="save-meet">
		<div id="save_meet_diag" style="padding:2px 8px 10px 8px;width:260px">
		
		<div style="border:1px solid #ccc;padding:4px;margin:10px 2px 2px 2px;font-size:12px">
			Save a <b>draft</b> of this app.
		</div>
	    <xf:submit submission="submit-draft" class="diagBtn dbSave">
	    	<xf:action ev:event="DOMActivate">
	    		<xf:setvalue ref="instance('meet')/mail" value="0"/>
	    	</xf:action>
	    </xf:submit>
	    <div style="border:1px solid #ccc;padding:4px;margin:20px 2px 2px 2px;font-size:12px">
			Save and send an <b>Email</b> to the team.<br/><br/>Include an optional meeting time:
			<xf:input ref="instance('meet')/meetstart" style="width:12em;font-size:11px"><xf:label style="width:70px">Start:</xf:label></xf:input>
			<xf:input ref="instance('meet')/meetend" style="width:12em;font-size:11px"><xf:label style="width:70px">End:</xf:label></xf:input>
		</div>
	    <xf:submit submission="submit-draft" class="diagBtn dbSend">
	    	<xf:action ev:event="DOMActivate">
	    		<xf:setvalue ref="instance('meet')/mail" value="1"/>
	    	</xf:action>
	    </xf:submit>
	    
	    
	    <div style="border:1px solid #ccc;padding:4px;margin:20px 2px 2px 2px;font-size:12px">
	    The app is <b>finished</b>. Save and <b>start</b> work on action items.</div>
		<xf:trigger class="diagBtn dbGo">
	    	<xf:action ev:event="DOMActivate">
		    	<script>
		    		ProcOutline.meetSave(model.context, model);
		    	</script>
		    </xf:action>
	    </xf:trigger>
	    
		</div>
	</ix:template>
	
    <ix:template name="step">
        <div style="padding:6px;width:470px">
        <xf:input ref="@id">
          <xf:label>Name:</xf:label>
          <xf:hint>This name needs to be unique.</xf:hint>
        </xf:input>
        <xf:textarea ref="description" style="width:20em;height:3em">
            <xf:label>De<span>s<xf:action ev:event="click">
            		<xf:duplicate ref=".." origin="instance('pal')/script"
            		exf:if="not(script)"/>
            	</xf:action></span>cription:</xf:label>
        </xf:textarea>
        <xf:select1 ref="@size">
        	<xf:label>Size:</xf:label>
           	<xf:item><xf:label>Small</xf:label><xf:value></xf:value></xf:item>
           	<xf:item><xf:label>Medium</xf:label><xf:value>M</xf:value></xf:item>
           	<xf:item><xf:label>Large</xf:label><xf:value>L</xf:value></xf:item>
        </xf:select1>
        <xf:input ref="@role">
            <xf:label>Role:</xf:label>
        </xf:input>
        <% if (useOrgs) { %>
        <table>
        <tr>
        	<td>&#160; &#160; </td>
        	<td style="width:210px">
        	<xf:select1 ref="@orgPosition">
				<xf:label>Org Position:</xf:label>
				<xf:item><xf:label>None</xf:label><xf:value></xf:value></xf:item>
				<xf:itemset nodeset="instance('opos')/position">
					<xf:label ref="@label"/>
					<xf:value ref="@id"/>
				</xf:itemset>	
			</xf:select1>
        	</td>
        	<td>&#160; from &#160;</td>
        	<td>
        	<xf:select1 ref="@orgAxis">
				<xf:item><xf:label>My Org</xf:label><xf:value></xf:value></xf:item>
				<xf:item><xf:label>Parent Org</xf:label><xf:value>PARENT</xf:value></xf:item>
				<xf:item><xf:label>Ancestor Org</xf:label><xf:value>ANCESTOR</xf:value></xf:item>
				<xf:item><xf:label>Mine or ancestor Org</xf:label><xf:value>ANCESTOR_OR_SELF</xf:value></xf:item>
				<xf:item><xf:label>Child Org</xf:label><xf:value>CHILD</xf:value></xf:item>
				<xf:item><xf:label>Mine or child Org</xf:label><xf:value>CHILD_OR_SELF</xf:value></xf:item>
				<xf:item><xf:label>Sibling Org</xf:label><xf:value>SIBLING</xf:value></xf:item>
			</xf:select1>
        	</td>
        </tr>
        </table>
        <% } %>


	    <xf:repeat nodeset="script">
	    	<div style="margin:10px 6px 6px 6px;border:1px solid #fff; padding: 2px;">
	        <xf:select1 ref="@on">
	            <xf:item><xf:label>Script On Enter</xf:label><xf:value>enter</xf:value></xf:item>
	           	<xf:item><xf:label>Script On Exit</xf:label><xf:value>exit</xf:value></xf:item>
            </xf:select1>
	        <xf:textarea ref="." style="width:424px;height:100px;font-size:10px;">
	        </xf:textarea>
	        <div style="color:red;font-size:10px;padding:2px;text-decoration:underline;cursor:pointer">Remove Script<xf:destroy 
	        	ref="." ev:event="click"/></div>
	        </div>
	    </xf:repeat>

        <ix:include template="_close"/>
        </div>
    </ix:template>


	<ix:template name="step-launch">
        <div style="padding:6px;width:350px">
        <xf:input ref="@id">
          <xf:label>Name:</xf:label>
          <xf:hint>This name needs to be unique.</xf:hint>
        </xf:input>
        <xf:textarea ref="description" style="width:20em;height:3em">
            <xf:label>Description:</xf:label>
        </xf:textarea>
        <xf:input ref="@flow" style="font-size:10px;width:20em">
            <xf:label>Flow:</xf:label>
            <u style="color:#009;padding-left:4px;cursor:pointer">pick<script ev:event="click">
            if (!uiParent._menu) {
            	var procFunc = function(evt) {
	            		model.setValue(".", this.uri, contextNode);
	            		model.refresh();
	            	};
            	uiParent._menu = new Menu(new TNavProcMenuModel("activeProcesses", procFunc), procFunc);
            	App.addDispose(uiParent._menu);
            }
            uiParent._menu.popUp(event.uiEvent);
            uiParent = null;
	        </script>
            </u>
       	</xf:input>
       	<ix:include template="_close"/>
        </div>
        
    </ix:template>

    <ix:template name="event">
        <div style="padding:6px;width:480px">
        <xf:input ref="@id">
          <xf:label>Name:</xf:label>
          <xf:hint>This name needs to be unique.</xf:hint>
        </xf:input>
        <xf:textarea ref="description" style="width:20em;height:3em">
            <xf:label>Description:</xf:label>
        </xf:textarea>
        <table style="height:185px;">
        <tr valign="top">
            <td style="border-right:1px solid #999;padding-right:8px">
            <xf:select1 ref="@mode" appearance="full">
                <xf:label>Wait for:</xf:label>
                <xf:item><xf:label>Period of time</xf:label><xf:value>wait</xf:value></xf:item>
                <xf:item><xf:label>Time of day</xf:label><xf:value>until-daily</xf:value></xf:item>
                <xf:item><xf:label>Day of week</xf:label><xf:value>until-weekly</xf:value></xf:item>
                <xf:item><xf:label>Day of month</xf:label><xf:value>until-monthly</xf:value></xf:item>
                <xf:item><xf:label>Rule Test</xf:label><xf:value>until-condition</xf:value></xf:item>
                <xf:action ev:event="xforms-value-changed" exf:if=". = 'wait'" >
                    <xf:destroy ref="../wait|../until"/>
                    <xf:duplicate ref=".." origin="instance('pal')/timer-modes/wait" />
                </xf:action>
                <xf:action ev:event="xforms-value-changed" exf:if="starts-with(., 'until-')">
                    <xf:destroy ref="../wait|../until"/>
                    <xf:duplicate ref=".." origin="instance('pal')/timer-modes/until[@type = substring-after(current(), '-')]" />
                </xf:action>
            </xf:select1>
            </td>
            <td style="padding-left:8px">
                <xf:action ev:event="xforms-value-changed">
                    <xf:setvalue ref="wait/@rev|until/@rev" value=". + 1"/>
                </xf:action>
                <xf:repeat nodeset="wait">
                    <xf:input ref="@days" style="width:5em"><xf:label style="width:55px">Days:</xf:label></xf:input>
                    <xf:input ref="@hours" style="width:5em"><xf:label style="width:55px">Hours:</xf:label></xf:input>
                    <xf:input ref="@minutes" style="width:5em"><xf:label style="width:55px">Minutes:</xf:label></xf:input>
                </xf:repeat>
                <xf:repeat nodeset="until[@type='daily']">
                    <xf:input ref="@at" style="width:6em">
                        <xf:label style="width:3em">At:</xf:label>
                        <xf:hint>Hour and minute. Ex - 11:30</xf:hint>
                    </xf:input>
                </xf:repeat>
                <xf:repeat nodeset="until[@type='weekly']">
                    <xf:select ref="@on">
                        <xf:label/>
                        <xf:item><xf:label>Sunday</xf:label><xf:value>sun</xf:value></xf:item>
                        <xf:item><xf:label>Monday</xf:label><xf:value>mon</xf:value></xf:item>
                        <xf:item><xf:label>Tuesday</xf:label><xf:value>tue</xf:value></xf:item>
                        <xf:item><xf:label>Wednesday</xf:label><xf:value>wed</xf:value></xf:item>
                        <xf:item><xf:label>Thursday</xf:label><xf:value>thu</xf:value></xf:item>
                        <xf:item><xf:label>Friday</xf:label><xf:value>fri</xf:value></xf:item>
                        <xf:item><xf:label>Saturday</xf:label><xf:value>sat</xf:value></xf:item>
                    </xf:select>
                    <xf:input ref="@at" style="width:6em">
                        <xf:label style="width:3em">At:</xf:label>
                        <xf:hint>Hour and minute. Ex - 11:30</xf:hint>
                    </xf:input>
                </xf:repeat>
                <xf:repeat nodeset="until[@type='monthly']">
                    <xf:select1 ref="@ord" appearance="full">
                        <xf:label/>
                        <xf:item><xf:label>
                            <table>
                            <tr>
                            <td>The</td>
                            <td>
                            <xf:select1 ref="../@alpha" style="width:5em">
                                <xf:label/>
                                <xf:item><xf:label>First</xf:label><xf:value>first</xf:value></xf:item>
                                <xf:item><xf:label>Second</xf:label><xf:value>second</xf:value></xf:item>
                                <xf:item><xf:label>Third</xf:label><xf:value>third</xf:value></xf:item>
                                <xf:item><xf:label>Fourth</xf:label><xf:value>fourth</xf:value></xf:item>
                                <xf:item><xf:label>Last</xf:label><xf:value>last</xf:value></xf:item>
								<xf:item><xf:label>Next</xf:label><xf:value>next</xf:value></xf:item>
                            </xf:select1>
                            </td>
                            <td>
                            <xf:select1 ref="../@on" style="width:8em">
                                <xf:label/>
                                <xf:item><xf:label>Day</xf:label><xf:value>day</xf:value></xf:item>
                                <xf:item><xf:label>Week day</xf:label><xf:value>wday</xf:value></xf:item>
                                <xf:item><xf:label>Sunday</xf:label><xf:value>sun</xf:value></xf:item>
                                <xf:item><xf:label>Monday</xf:label><xf:value>mon</xf:value></xf:item>
                                <xf:item><xf:label>Tuesday</xf:label><xf:value>tue</xf:value></xf:item>
                                <xf:item><xf:label>Wednesday</xf:label><xf:value>wed</xf:value></xf:item>
                                <xf:item><xf:label>Thursday</xf:label><xf:value>thu</xf:value></xf:item>
                                <xf:item><xf:label>Friday</xf:label><xf:value>fri</xf:value></xf:item>
                                <xf:item><xf:label>Saturday</xf:label><xf:value>sat</xf:value></xf:item>
                            </xf:select1>
                            </td>
                            </tr>
                            </table>
                            </xf:label>
                            <xf:value>alpha</xf:value>
                        </xf:item>
                        <xf:item>
                            <xf:label>
                                <table>
                                <tr>
                                <td>On</td>
                                <td>
                                <xf:select1 ref="../@unit" style="width:5em">
                                    <xf:label/>
                                    <xf:item><xf:label>Day</xf:label><xf:value>day</xf:value></xf:item>
                                    <xf:item><xf:label>Week day</xf:label><xf:value>wday</xf:value></xf:item>
                                </xf:select1>
                                </td>
                                <td>
                                <xf:input ref="../@number" style="width:3em">
                                    <xf:label/>
                                    <xf:hint>Between 1-31 depending on days/weekdays in month.</xf:hint>
                                </xf:input>
                                </td>
                                </tr>
                                </table>
                            </xf:label>
                            <xf:value>number</xf:value>
                        </xf:item>
                    </xf:select1>
                    <xf:input ref="@at" style="width:6em">
                         <xf:label style="width:3em">At:</xf:label>
                         <xf:hint>Hour and minute. Ex - 11:30</xf:hint>
                    </xf:input>
                </xf:repeat>
                <xf:repeat nodeset="until[@type='condition']">
                    <xf:textarea ref="condition" style="width:15em;height:5em">
                        <xf:label style="width:4em">Rule:</xf:label>
                    </xf:textarea>
                </xf:repeat>
            </td>
        </tr>
        </table>
        <ix:include template="_close"/>
        </div>
    </ix:template>

    <ix:template name="start">
        <div style="padding:6px;width:350px">
        <xf:input ref="@id">
          <xf:label>Name:</xf:label>
          <xf:hint>This name needs to be unique.</xf:hint>
        </xf:input>
        <xf:select1 ref="/flow/iw:type/@icon">
            <xf:label>Icon:</xf:label>
            <xf:item><xf:label><div class="selDeco defIco"/></xf:label><xf:value>def</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco jpg_Ico"/></xf:label><xf:value>jpg_</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco txt_Ico"/></xf:label><xf:value>txt_</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco html_Ico"/></xf:label><xf:value>html_</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco linkIco"/></xf:label><xf:value>link</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco msg_Ico"/></xf:label><xf:value>msg_</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco mov_Ico"/></xf:label><xf:value>mov_</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco ppt_Ico"/></xf:label><xf:value>ppt_</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco doc_Ico"/></xf:label><xf:value>doc_</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco xls_Ico"/></xf:label><xf:value>xls_</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco vsd_Ico"/></xf:label><xf:value>vsd_</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco swf_Ico"/></xf:label><xf:value>swf_</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco xml_Ico"/></xf:label><xf:value>xml_</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco xrpt_Ico"/></xf:label><xf:value>xrpt_</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco kb_Ico"/></xf:label><xf:value>kb_</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco groupIco"/></xf:label><xf:value>group</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco everyoneIco"/></xf:label><xf:value>everyone</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco userIco"/></xf:label><xf:value>user</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco mp3_Ico"/></xf:label><xf:value>mp3_</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco zip_Ico"/></xf:label><xf:value>zip_</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco milestoneIco"/></xf:label><xf:value>milestone</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco projectIco"/></xf:label><xf:value>project</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco psd_Ico"/></xf:label><xf:value>psd_</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco ai_Ico"/></xf:label><xf:value>ai_</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco fla_Ico"/></xf:label><xf:value>fla_</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco pdf_Ico"/></xf:label><xf:value>pdf_</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco xformIco"/></xf:label><xf:value>xform</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco task1Ico"/></xf:label><xf:value>task1</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco task2Ico"/></xf:label><xf:value>task2</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco task3Ico"/></xf:label><xf:value>task3</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco task4Ico"/></xf:label><xf:value>task4</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco star1Ico"/></xf:label><xf:value>star1</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco star2Ico"/></xf:label><xf:value>star2</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco star3Ico"/></xf:label><xf:value>star3</xf:value></xf:item>
            <xf:item><xf:label><div class="selDeco bookIco"  /></xf:label><xf:value>book</xf:value></xf:item>
			<xf:item><xf:label><div class="selDeco bugIco"   /></xf:label><xf:value>bug</xf:value></xf:item>
			<xf:item><xf:label><div class="selDeco carIco"   /></xf:label><xf:value>car</xf:value></xf:item>
			<xf:item><xf:label><div class="selDeco pcIco"    /></xf:label><xf:value>pc</xf:value></xf:item>
			<xf:item><xf:label><div class="selDeco chatIco"  /></xf:label><xf:value>chat</xf:value></xf:item>
			<xf:item><xf:label><div class="selDeco drinkIco" /></xf:label><xf:value>drink</xf:value></xf:item>
			<xf:item><xf:label><div class="selDeco cashIco"  /></xf:label><xf:value>cash</xf:value></xf:item>
			<xf:item><xf:label><div class="selDeco medalIco" /></xf:label><xf:value>medal</xf:value></xf:item>
			<xf:item><xf:label><div class="selDeco findIco"  /></xf:label><xf:value>find</xf:value></xf:item>
			<xf:item><xf:label><div class="selDeco pillIco"  /></xf:label><xf:value>pill</xf:value></xf:item>
			<xf:item><xf:label><div class="selDeco boltIco"  /></xf:label><xf:value>bolt</xf:value></xf:item>
			<xf:item><xf:label><div class="selDeco phoneIco" /></xf:label><xf:value>phone</xf:value></xf:item>   
			<xf:item><xf:label><div class="selDeco flagIco"  /></xf:label><xf:value>flag</xf:value></xf:item>
			<xf:item><xf:label><div class="selDeco writeIco" /></xf:label><xf:value>write</xf:value></xf:item>
			<xf:item><xf:label><div class="selDeco sheildIco"/></xf:label><xf:value>sheild</xf:value></xf:item>
			<xf:item><xf:label><div class="selDeco worldIco" /></xf:label><xf:value>world</xf:value></xf:item>
			<xf:item><xf:label><div class="selDeco truckIco" /></xf:label><xf:value>truck</xf:value></xf:item>
			<xf:item><xf:label><div class="selDeco clockIco" /></xf:label><xf:value>clock</xf:value></xf:item>
			<xf:item><xf:label><div class="selDeco homeIco " /></xf:label><xf:value>home</xf:value></xf:item>
			
			<xf:item><xf:label><div class="selDeco grantsIco " /></xf:label><xf:value>grants</xf:value></xf:item>
			<xf:item><xf:label><div class="selDeco humansIco " /></xf:label><xf:value>humans</xf:value></xf:item>
			<xf:item><xf:label><div class="selDeco animalIco " /></xf:label><xf:value>animal</xf:value></xf:item>
			<xf:item><xf:label><div class="selDeco conflictIco " /></xf:label><xf:value>conflict</xf:value></xf:item>
			<xf:item><xf:label><div class="selDeco researchIco " /></xf:label><xf:value>research</xf:value></xf:item>
			
        </xf:select1>
        <xf:textarea ref="description" style="width:20em;height:3em">
            <xf:label>Description:</xf:label>
        </xf:textarea>
        <ix:include template="_close"/>
        </div>
    </ix:template>

    <ix:template name="switch">
        <div style="padding:6px;width:350px">
        <xf:input ref="@id">
          <xf:label>Name:</xf:label>
        </xf:input>
        <xf:textarea ref="description" style="width:20em;height:3em">
            <xf:label>Description:</xf:label>
        </xf:textarea>
		<% if (useRuleset) { %>
		<xf:repeat nodeset="ruleset">
	        <xf:input ref="@src" extensions="rule" note="Optional: drop a file here">
	          	<xf:label>Rule Set:</xf:label>
	          	<xf:action ev:event="xforms-value-changed">
	          		<xf:toggle><xf:case value="if(.='','conditions','ruleset')"/></xf:toggle>
	          		<xf:rebuild model="fmodel"/>
	          	</xf:action>
	        </xf:input>
		</xf:repeat>
		<% } %>
		<div style="padding-top:4px;clear:both">
		<exf:variable name="pCount" value="count(path)"/>
		<xf:switch>
			<xf:case id="ruleset" exf:if="ruleset/@src != ''">
				<exf:variable name="rulRoot" value="document(ruleset/@src)/rl:ruleset"/>
				<xf:select1 ref="ruleset/@rule" style="width:8em">
		          	<xf:label>&#160; Sub Rule:</xf:label>
		          	<xf:item>
		          		<xf:label style="color:#666;font-style:italic">Main</xf:label>
		          		<xf:value/>
		          	</xf:item>
		          	<xf:item separator="true"/>
		          	<xf:itemset nodeset="$rulRoot/rl:rule[@id != 'default']">
		          		<xf:label ref="@id"/>
		          		<xf:value ref="@id"/>
		          	</xf:itemset>
		        </xf:select1>
		        <div class="sectHead">Rule Mapping:</div>
		        <div>
		        	<ix:attr name="style" value="if($pCount > 0, 'display:none', '')"/>
		        	There are currently no outbound paths to set map rules on.
		        </div>
		        <xf:repeat nodeset="path">
		            <div style="border:1px solid #ccc;margin:2px">
		                <exf:variable name="pos" value="position()"/>
		                <xf:input ref="label">
		                    <xf:label>
		                        <xf:output value="if($pos = 1, 'If:', if($pos = $pCount, 'Else:', 'Else If:'))"/>
		                    </xf:label>
		                    <xf:hint><xf:output value="concat('To: ', ../@to)" /></xf:hint>
		                </xf:input>
		                <xf:select1 ref="condition">
		                    <ix:attr name="style" value="if($pos = $pCount, 'display:none', '')"/>
		                    <xf:label>&#160; (Return)</xf:label>
		                    <xf:item><xf:label>- Select -</xf:label><xf:value/></xf:item>
		                    <xf:itemset nodeset="$rulRoot/rl:returns/rl:return">
				          		<xf:label ref="@id"/>
				          		<xf:value ref="@id"/>
				          	</xf:itemset>
		                </xf:select1>
		            </div>
		        </xf:repeat>
			</xf:case>
			<xf:case id="conditions">
				<div class="sectHead">Conditional Rule Tests:</div>
				<div>
		        	<ix:attr name="style" value="if($pCount > 0, 'display:none', '')"/>
		        	There are currently no outbound paths to set conditional rules on.
		        </div>
		       	<xf:repeat nodeset="path">
		            <div style="border: 1px solid #ccc;margin:2px">
		                <exf:variable name="pos" value="position()"/>
		                <xf:input ref="label">
		                    <xf:label>
		                        <xf:output value="if($pos = 1, 'If:', if($pos = $pCount, 'Else:', 'Else If:'))"/>
		                    </xf:label>
		                    <xf:hint><xf:output value="concat('To: ', ../@to)" /></xf:hint>
		                </xf:input>
		                <xf:input ref="condition">
		                    <ix:attr name="style" value="if($pos = $pCount, 'display:none', '')"/>
		                    <xf:label>&#160; (Rule)</xf:label>
		                </xf:input>
		            </div>
		        </xf:repeat>
			</xf:case>
		</xf:switch>
        </div>
        <ix:include template="_close"/>
        </div>
    </ix:template>

    <ix:template name="end">
        <div style="padding:6px">
        <xf:input ref="@id">
          <xf:label>Name:</xf:label>
          <xf:hint>This name needs to be unique.</xf:hint>
        </xf:input>
        <xf:textarea ref="description" style="width:20em;height:3em">
            <xf:label>Description:</xf:label>
        </xf:textarea>
        <ix:include template="_close"/>
        </div>
    </ix:template>
    
    <ix:template name="group">
        <div style="padding:6px">
        <xf:input ref="@id">
          <xf:label>Name:</xf:label>
          <xf:hint>This name needs to be unique.</xf:hint>
        </xf:input>
        <xf:textarea ref="description" style="width:20em;height:3em">
            <xf:label>Description:</xf:label>
        </xf:textarea>
        <ix:include template="_close"/>
        </div>
    </ix:template>
    
  	<ix:template name="enter">
        <div style="padding:6px">
        <xf:input ref="label">
          <xf:label>Name:</xf:label>
        </xf:input>
        <xf:textarea ref="description" style="width:20em;height:3em">
            <xf:label>Description:</xf:label>
        </xf:textarea>
        <ix:include template="_close"/>
        </div>
    </ix:template>
    
 	<ix:template name="exit">
        <div style="padding:6px">
        <xf:input ref="label">
          <xf:label>Name:</xf:label>
        </xf:input>
        <xf:textarea ref="description" style="width:20em;height:3em">
            <xf:label>Description:</xf:label>
        </xf:textarea>
        <ix:include template="_close"/>
        </div>
    </ix:template>

    <ix:template name="path">
        <div style="padding:6px">
        <xf:input ref="label">
            <xf:label>Label:</xf:label>
        </xf:input>
        <ix:include template="_close"/>
        </div>
    </ix:template>

    <ix:template name="note">
        <div style="padding:6px;width:350px">
        <xf:textarea ref="." style="width:20em;height:4em">
            <xf:label>Notes:</xf:label>
        </xf:textarea>
        <ix:include template="_close"/>
        </div>
    </ix:template>
    
    <ix:template name="attrManager" class="attrManager">
    	<script>
    		var dnd = dndGetCanvas(document.body);
    		dnd.addDNDType(new WfAttrDNDType());
    	</script>
    	<table class="attrManager">
    	<tr>
    		<th>Name</th>
    		<th>Type</th>
    		<th/>
    	</tr>
    	<xf:repeat nodeset="iw:data/iw:attr">
    		<tr valign="top">
    			<ix:attr name="class" value="if(position() &lt; 13,'projAtt','')"/>
    			<td>
    			<xf:input ref="@name" style="width:7em;font-size:11px;border:1px solid #999"><xf:label/></xf:input>
    			</td>
    			<td>
    			<xf:select1 ref="@type" style="width:6.2em;font-size:11px">
                    <xf:label/>
                    <xf:item><xf:label>Text</xf:label><xf:value>xsd:string</xf:value></xf:item>
                    <xf:item><xf:label>Number</xf:label><xf:value>xsd:float</xf:value></xf:item>
                    <xf:item><xf:label>Yes/No</xf:label><xf:value>xsd:boolean</xf:value></xf:item>
                    <xf:item><xf:label>Date</xf:label><xf:value>xsd:date</xf:value></xf:item>
                    <xf:item><xf:label>Date&#160;&amp;&#160;Time</xf:label><xf:value>xsd:dateTime</xf:value></xf:item>
                    <xf:item><xf:label>Dollars&#160;($)</xf:label><xf:value>ix:currencyUSD</xf:value></xf:item>
                    <xf:item><xf:label>Percent&#160;(%)</xf:label><xf:value>ix:percent</xf:value></xf:item>
                    <xf:item><xf:label>Email</xf:label><xf:value>ix:email</xf:value></xf:item>
                    <xf:item><xf:label>File</xf:label><xf:value>ix:file</xf:value></xf:item>
                    <xf:item><xf:label>Web Link</xf:label><xf:value>ix:http</xf:value></xf:item>
                    <xf:item><xf:label>Select List</xf:label><xf:value>xsd:NMTOKEN</xf:value></xf:item>
                    <xf:item><xf:label>Multi-Select</xf:label><xf:value>xsd:NMTOKENS</xf:value></xf:item>
                    <xf:item><xf:label>Activity</xf:label><xf:value>ix:activity</xf:value></xf:item>
                </xf:select1>
                <div>
                <u style="cursor:pointer">Options<script ev:event="click">
                		if (model.getValue("count(item)", contextNode) == 0) {
                			model.duplicateNode("instance('pal')/item", ".", null, contextNode);
                		}
                		var diag = xfTemplateDialog("Select Attribute: " + model.getValue("@name",contextNode), true, 
                			document.body, model.getForm(), "attrSelectOpts", contextNode, false, null, App.chromeHelp);
                		diag.show(220, 330);
	                </script></u><ix:attr name="style" value="if(contains('xsd:NMTOKENS', @type),'','display:none')"/></div>
    			</td>
    			<td>
    			<div class="attrDrag mb_inpuIco" title="Drag Attribute">&#160;<script>
    				var dnd = dndGetCanvas(document.body);
    				uiParent._ctxNode = App.disposableNode(contextNode);
    				dnd.makeDraggable(uiParent, "dndWfAttr");
    			</script><ix:attr name="style" value="if(@name='','display:none','')"/></div>
    			</td>
    		</tr>
    	</xf:repeat>
    	</table>
    	<u class="attrLink"><xf:duplicate ref="iw:data" ev:event="click"
    		origin="instance('pal')/iw:attr"/><ix:attr name="style" 
    		value="if(count(iw:data/iw:attr) &lt; 100,'','display:none')"/>Add Attribute</u>

    	<span><ix:attr name="style" 
    		value="if(iw:data/iw:attr,'','display:none')"/> | <u class="attrLink"><xf:destroy 
    		ev:event="click" ref="iw:data/iw:attr[last()]" />Remove</u></span>
    	
    	<% if (useRuleset) { %>
    	<p><ix:attr name="style" 
    		value="if(iw:data/iw:attr,'','display:none')"/>
   		<u class="attrLink"><xf:action ev:event="click">
   				<script>
   				PmCanvas.mainCanvas.saveRuleset();
   				</script>
   			</xf:action>Create new ruleset &gt;</u>
   		</p>
    	<% } %>
    </ix:template>
    
    <ix:template name="attrSelectOpts">
    	<div style="padding:6px;width:350px">
	    	<table class="attrOpt">
	    	<tr>
	    		<th>Label</th>
	    		<th>Value</th>
	    		<th/>
	    	</tr>
	    	<xf:repeat nodeset="item">
	    	<tr>
	    		<td><xf:input style="width:18em" ref="@label"/></td>
	    		<td><xf:input style="width:7em" ref="@value">
	    			<xf:hint>Avoid spaces in the value.</xf:hint>
	    		</xf:input></td>
	    		<td class="attrDel"><div class="attrDel">X<xf:destroy ref="." ev:event="click"/></div></td>
			</tr>
	    	</xf:repeat>
    		<tr>
      			<td colspan="3"><u class="attrLink"><xf:duplicate ref="." ev:event="click"
	    			origin="instance('pal')/item"/>Add Option</u></td>
        	</tr>
	    	</table>
	    	
    	</div>
    </ix:template>
    
    
    <ix:template name="entityManager" class="entManager">
    <script>
   		var dnd = dndGetCanvas(document.body);
   		dnd.addDNDType(new EntFormDropDNDType());
   	</script>
    <div style="margin-top:10px">
   		<table class="entManager">
   		<xf:repeat nodeset="iw:data/iw:entity">
   		<tr valign="top">
   			<td style="padding:4px 6px 2px 2px" nowrap="nowrap">
   			<xf:select1 ref="@relation" style="font-size:10px;color:#666">
   				<xf:label>Has</xf:label>
                <xf:item><xf:label>One&#160;or&#160;More</xf:label><xf:value>1orMore</xf:value></xf:item>
                <xf:item><xf:label>One</xf:label><xf:value>1</xf:value></xf:item>
   			</xf:select1>
   			<xf:select1 ref="@condition" style="font-size:10px;color:#666">
   				<xf:label>&#160;</xf:label>
                <xf:item><xf:label>New&#160;or&#160;Existing</xf:label><xf:value>newOrExisting</xf:value></xf:item>
                <xf:item><xf:label>Existing</xf:label><xf:value>existing</xf:value></xf:item>
                <xf:item><xf:label>New</xf:label><xf:value>new</xf:value></xf:item>
   			</xf:select1>
   			<xf:select1 ref="@type" style="font-size:10px">
   				<xf:label>&#160;</xf:label>
                <xf:item><xf:label>-Select&#160;Entity-</xf:label><xf:value/></xf:item>
                <xf:itemset nodeset="instance('entIns')/node">
                	<xf:label ref="@uri"/>
                	<xf:value ref="@id"/>
                </xf:itemset>
                <xf:setvalue ev:event="xforms-value-changed" ref="../@name" 
              		value="instance('entIns')/node[current()/../@type = @id]/@uri" exf:if=". = ''"/>
   			</xf:select1>
   			<xf:input style="width:9em" ref="@name">
    			<xf:label>Called</xf:label>
    			<xf:hint>Name of relationship.</xf:hint>
    		</xf:input>
   			</td>
   			<td class="attrDel"><div class="attrDel" title="Remove">X<xf:destroy ref="." ev:event="click"/></div></td>
   			<td>
   			<div class="attrDrag mb_inpuIco" title="Drag Entity">&#160;<script>
   				var dnd = dndGetCanvas(document.body);
   				uiParent._ctxNode = App.disposableNode(contextNode);
   				dnd.makeDraggable(uiParent, "dndEntForm");
   			</script><ix:attr name="style" value="if(@name='','display:none','')"/></div>
   			</td>
   		</tr>
   		</xf:repeat>
   		<tr>
   		<td colspan="3"><u class="attrLink"><xf:action ev:event="click">
   			<xf:duplicate ref="iw:data" origin="instance('pal')/iw:entity"/>
   			</xf:action>Add Relationship</u></td>
  		</tr>
   		</table>
   	</div>
    </ix:template>
    
    <ix:template name="_close">
		<xf:trigger class="diagBtn dbDone" style="margin-top:10px">
        	<xf:dispatch ev:event="DOMActivate" name="xforms-close"/>
        </xf:trigger>
	</ix:template>

</form>
