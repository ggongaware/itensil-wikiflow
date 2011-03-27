<%@ page import="itensil.security.User"%>

<form xmlns="http://www.w3.org/2002/06/xhtml2"
    xmlns:xf="http://www.w3.org/2002/xforms"
    xmlns:exf="http://www.exforms.org/exf/1-0"
    xmlns:ev="http://www.w3.org/2001/xml-events"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
    xmlns:ix="http://itensil.com/ns/xforms"
    xmlns:ir="http://itensil.com/repository"
    xmlns:d="DAV:">

    <xf:model id="fmodel">

        <xf:instance id="ins">
            <invite>
                <name/>
                <email/>
                <guest/>
                <log/>
                <comment/>
            </invite>
        </xf:instance>

        <xf:bind nodeset="name" constraint=". != ''"/>
        <xf:bind nodeset="email" type="ix:email" constraint=". != ''"/>
        <xf:bind nodeset="log" relevant="../guest = 1"/>

        <xf:submission id="submission" replace="none" method="post" action="../uspace/invite">
            <xf:action ev:event="xforms-submit-done">
                <xf:toggle case="submsg"/>
                <script>
                	var resRoot = model.getSubmitResponse().documentElement;
                	var bodyText = Xml.stringForNode(Xml.matchOne(resRoot, "body"));
                	model.getUiElementById('invBody')[SH.is_ie ? 'innerText' : 'innerHTML'] = bodyText;
                	var link = model.getUiElementById('invLink');
                	link.href = "mailto:" + resRoot.getAttribute("email") + "?subject=Invitation&amp;body=" + Uri.escape(bodyText);
                </script>
			</xf:action>
			<xf:action ev:event="xforms-submit-error">
				<xf:message level="modal">Problem saving...</xf:message>
			</xf:action>
		</xf:submission>

    </xf:model>

	<ix:template name="_guest">
	<table>
	<tr>
	<td>
		<xf:select ref="guest">
			<xf:label>&#160;</xf:label>
			<xf:item>
				<xf:label>Invite as <b>Guest</b></xf:label>
				<xf:value>1</xf:value>
			</xf:item>
		</xf:select>
	</td>
	<td>
		<xf:select ref="log">
			<xf:item>
				<xf:label>Guest access to <i>activiy logs</i></xf:label>
				<xf:value>1</xf:value>
			</xf:item>
		</xf:select>
	</td>
	</tr>
	</table>
    </ix:template>
    
    <div style="padding:6px;width:420px">

        <xf:input ref="name">
          <xf:label>Name:</xf:label>
        </xf:input>

        <xf:input ref="email">
            <xf:label>Email:</xf:label>
            <xf:hint>A password for this person will be sent by email.</xf:hint>
        </xf:input>
        
        
        <xf:textarea ref="comment" style="height:5em;width:15em;">
            <xf:label>Comment:</xf:label>
            <xf:hint>Comment note sent with the invitation email.</xf:hint>
        </xf:textarea>
<% 	User user = (User)request.getUserPrincipal();
	if (user != null && user.getUserSpace().getFeatures().contains("guests"))  {
 %> 
        <ix:include template="_guest"/>
<% } %>

	        <xf:switch>
            <xf:case id="hide">
            	<xf:submit submission="submission" class="diagBtn dbSend" style="margin: 10px 0px 50px 0px"/>
            </xf:case>
            <xf:case id="submsg">
            <div style="position: absolute;top: 20px;left: 20px;border: 2px solid #666;background-color:#ff9;padding:10px;width:370px;">
            	<p>An account is now available for this user, please click <b>Email this invitation</b> or cut and paste this information to send to this user.</p>
            	<a id="invLink" href="mailto:xxx">Click here to Email this invitation message</a>
            	<hr/>
				<pre id="invBody"></pre>
				
            </div>
            </xf:case>
        </xf:switch>

    </div>
    
    
    

</form>
