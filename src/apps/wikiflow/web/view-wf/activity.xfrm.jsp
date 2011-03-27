<%@ page import="itensil.security.User" contentType="text/xml;charset=UTF-8" %>
<% 	User user = (User)request.getUserPrincipal();
	boolean useOrgs = false;
	if (user != null) {
		useOrgs = user.getUserSpace().getFeatures().contains("orgs");
	}
%>
<form xmlns="http://www.w3.org/2002/06/xhtml2"
    xmlns:xf="http://www.w3.org/2002/xforms"
    xmlns:exf="http://www.exforms.org/exf/1-0"
    xmlns:ev="http://www.w3.org/2001/xml-events"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
    xmlns:ix="http://itensil.com/ns/xforms">

    <xf:model id="fmodel">

        <xf:instance id="ins">
            <activity xmlns=""/>
        </xf:instance>

		<xf:bind nodeset="@name" type="ix:fileName" constraint=". != ''"/>
        <xf:bind type="xsd:date" nodeset="@dueDate"/>
        <xf:bind type="xsd:date" nodeset="@startDate"/>
        <xf:bind type="ix:userGroup" nodeset="@contextGroup"/>

        <xf:submission id="submission" replace="none" method="post" action="../act/setProps">
            <xf:action ev:event="xforms-submit-done">
                <xf:close />
			</xf:action>
			<xf:action ev:event="xforms-submit-error">
				<xf:message level="modal">Problem saving...</xf:message>
			</xf:action>
		</xf:submission>

    </xf:model>

    <div style="padding:6px;width:350px">

        <xf:input ref="@name">
          <xf:label>Name:</xf:label>
          <xf:hint>This name needs to be unique.</xf:hint>
        </xf:input>

        <xf:textarea ref="@description" style="width:20em;height:3em">
            <xf:label>Description:</xf:label>
            <xf:hint>Optional.</xf:hint>
        </xf:textarea>

		<xf:input ref="@startDate" style="width:10em">
          <xf:label>Start:</xf:label>
          <xf:hint>Optional.</xf:hint>
        </xf:input>

        <xf:input ref="@dueDate" style="width:10em">
          <xf:label>Due:</xf:label>
          <xf:hint>Optional.</xf:hint>
        </xf:input>

<% if (useOrgs) { %>
		<xf:input ref="@contextGroup">
          <xf:label>Activity Org:</xf:label>
          <xf:hint>Optional.</xf:hint>
        </xf:input>
<% } %>

        <xf:submit submission="submission" class="diagBtn dbSave"/>

    </div>

</form>

