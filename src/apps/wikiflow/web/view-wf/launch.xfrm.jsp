<%@ page import="itensil.repository.RepositoryHelper, itensil.security.User, itensil.io.StreamUtil" contentType="text/xml" %>
<% 
	
	RepositoryHelper.beginTransaction();
    RepositoryHelper.useReadOnly();
    String mount = RepositoryHelper.getPrimaryRepository().getMount();
    try {
    	StreamUtil.copyStream(
    		RepositoryHelper.loadContent(mount + "/Launch.xfrm"),
    		out);
    	RepositoryHelper.commitTransaction();
   		RepositoryHelper.closeSession();
    	return;
    } catch (Exception ex) {}

    
   	User user = (User)request.getUserPrincipal();
	boolean useOrgs = false;
	if (user != null) {
		useOrgs = user.getUserSpace().getFeatures().contains("orgs");
	}
	
	RepositoryHelper.commitTransaction();
    RepositoryHelper.closeSession();
%>
<form xmlns="http://www.w3.org/2002/06/xhtml2"
    xmlns:xf="http://www.w3.org/2002/xforms"
    xmlns:exf="http://www.exforms.org/exf/1-0"
    xmlns:ev="http://www.w3.org/2001/xml-events"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
    xmlns:ix="http://itensil.com/ns/xforms">

    <xf:model id="fmodel">

        <xf:instance id="ins">

            <launch xmlns="">
                <flow/>
                <master-flow/>
                <name/>
                <description/>
                <startDate/>
                <dueDate/>
                <priority/>
                <parent/>
                <parentStep/>
                <project/>
                <proj-lock/>
                <meet/>
                <contextGroup/>
            </launch>

        </xf:instance>
        
        <xf:instance id="proj" src="../proj/listProjects?uri=/home/project"/>

        <xf:bind nodeset="name" type="ix:fileName" constraint=". != ''"/>
        <xf:bind type="xsd:date" nodeset="startDate|dueDate" relevant="../meet != 1"/>
        <xf:bind nodeset="project" relevant="../parent = '' and ../meet != 1" readonly="../proj-lock = 1"/>
        <xf:bind type="ix:userGroup" nodeset="contextGroup" relevant="../meet != 1"/>

        <xf:submission id="submission" replace="none" method="post" action="../act/launch">
            <xf:action ev:event="xforms-submit-done">
                <xf:toggle case="submsg"/>
			</xf:action>
			<xf:action ev:event="xforms-submit-error">
				<xf:message level="modal">Problem saving...</xf:message>
			</xf:action>
		</xf:submission>

    </xf:model>

    <div style="padding:8px;width:350px">

        <xf:input ref="name">
          <xf:label>Name:</xf:label>
          <xf:hint>This name needs to be unique.</xf:hint>
        </xf:input>

        <xf:textarea ref="description" style="width:20em;height:3em">
            <xf:label>Description:</xf:label>
            <xf:hint>Optional.</xf:hint>
        </xf:textarea>

        <xf:input ref="dueDate" style="width:10em">
          <xf:label>Due:</xf:label>
          <xf:hint>Optional.</xf:hint>
        </xf:input>
        
        <xf:select1 ref="project">
        	<xf:label>Project:</xf:label>
        	<xf:item><xf:label>-optional-</xf:label><xf:value/></xf:item>
        	<xf:itemset nodeset="instance('proj')/node">
        		<xf:label ref="@uri"/>
        		<xf:value ref="@id"/>
        	</xf:itemset>
        </xf:select1>

<% if (useOrgs) { %>
		<xf:input ref="contextGroup">
          <xf:label>Activity Org:</xf:label>
          <xf:hint>Optional.</xf:hint>
        </xf:input>
<% } %>
        
        <div id="roles" class="launchRoles"></div>

        <xf:switch>
            <xf:case id="hide">
            	<xf:submit submission="submission" class="diagBtn dbLaunch" style="margin-top:10px"/>
            </xf:case>
            <xf:case id="submsg">
            <div style="position: absolute;top: 20px;left: 20px;border: 2px solid #666;background-color: #ff9;padding:10px;width:300px;">
				<div>
				Please wait, loading...
				</div>
            </div>
            </xf:case>
        </xf:switch>
        
        <div style="height:60px;">&#160;</div>
        
    </div>

</form>
