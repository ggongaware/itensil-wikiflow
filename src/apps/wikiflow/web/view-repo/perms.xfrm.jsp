<%@ page import="itensil.security.User" contentType="text/xml" %>
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
		
		<xf:instance id="ins" />
		
		<% if (useOrgs) { %>
        <xf:instance id="opos" src="../fil/home/org-positions.xml">
        	<org-positions xmlns=""/>
        </xf:instance>
        <% } %>
        
        <xf:instance id="pal">
        	<pal xmlns="">
        		<perm level="1" user="" inherit="1" change="0"/>
        		<perm level="1" group="" inherit="1" change="0"/>
        		<perm level="1" relative="1" position="" axis="" inherit="1" change="0"/>
        	</pal>
        </xf:instance>
        
        <xf:bind type="ix:userGroup" nodeset="@contextGroup"  readonly="instance('ins')/@manageable != 1"/>
        <xf:bind type="ix:user" nodeset="@owner" readonly="1"/>
        <xf:bind type="ix:userGroup" nodeset="perm/@group" readonly="instance('ins')/@manageable != 1"/>
        <xf:bind type="ix:user" nodeset="perm/@user" readonly="instance('ins')/@manageable != 1"/>
        <xf:bind nodeset="perm/@inherit" relevant="instance('ins')/@collection = 1" readonly="instance('ins')/@manageable != 1"/>
        <xf:bind nodeset="perm/@axis" readonly="instance('ins')/@manageable != 1"/>
        <xf:bind nodeset="perm/@position" readonly="instance('ins')/@manageable != 1"/>
		
		<xf:submission id="submission" replace="none" method="post" action="../shell/setPerms">
			<xf:action ev:event="xforms-submit-done">
                <xf:close />
			</xf:action>
			<xf:action ev:event="xforms-submit-error">
				<xf:message level="modal">Problem saving...</xf:message>
			</xf:action>
		</xf:submission>
		
   	</xf:model>

<ix:template name="_user">
<xf:input ref="@user"/>
</ix:template>

<ix:template name="_group">
<xf:input ref="@group" everyone="1"/>
</ix:template>

<ix:template name="_relative">
<table class="prelative">
    <tr>
     <td>
      	<xf:select1 ref="@position" style="font-size:10px;">
		<xf:item><xf:label>None</xf:label><xf:value></xf:value></xf:item>
		<xf:itemset nodeset="instance('opos')/position">
			<xf:label ref="@label"/>
			<xf:value ref="@id"/>
		</xf:itemset>
		</xf:select1>
    </td>
    <td> in </td>
    <td>
      	<xf:select1 ref="@axis" style="font-size:10px;">
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
</ix:template>
	
   	<div style="padding:6px;width:500px;position:relative">
   	    <script>
			model._fpCanMan = model.getValue("@manageable") == "1";
			
		</script>
   	    <div class="permDiag">
   	    
   	    <xf:input ref="@owner" >
          <xf:label>Owner:</xf:label>
        </xf:input>
        
   	    <% if (useOrgs) { %>
		<xf:input ref="@contextGroup">
          <xf:label>Access Org:</xf:label>
          <xf:hint>Optional.</xf:hint>
        </xf:input>
		<% } %>
		
		<br/>
		
		<table class="perms" cellpadding="0" cellspacing="0">
		<tbody>
		<tr>
		<th>User/Group</th><th>Access Level</th><th><xf:output value="if(@collection = 1, 'Scope', '')"/></th><th>&#160;</th>
		</tr>
		<xf:repeat nodeset="perm[not(@owner) and @change != 2]">
		<tr>
		<td class="user">
<ix:include template="_user" exf:if="@user"/>
<ix:include template="_group" exf:if="@group"/>
<ix:include template="_relative" exf:if="@relative"/>	
<%--
<div class="who"><div class="icon userIco">&nbsp;</div><span class="name">Steve Johanson (owner)</span></div>

--%>
</td>
		<td class="level">
		<script>
		var fp = new FilePerm(contextNode);
		fp.render(uiParent, model._fpCanMan);
		</script>
	<%-- <div style="background-position: 0px -112px;" class="perm">&#160;</div>
	--%> </td>
		<td><xf:select1 ref="@inherit" style="font-size:10px;color:#333">
			<xf:item><xf:label>Include<br/>subfolders</xf:label><xf:value>1</xf:value></xf:item>
			<xf:item><xf:label>This only</xf:label><xf:value>0</xf:value></xf:item>
		</xf:select1></td>
		<td class="attrDel"><div class="attrDel" title="Remove">X<xf:action ev:event="click">
			<xf:setvalue ref="@change" value="2"/>
			<xf:rebuild model="fmodel"/>
		</xf:action><ix:attr name="style" value="if(../@manageable = 1, '', 'display:none')"/></div></td>
		</tr>
		</xf:repeat>		
		</tbody>
		</table>
<div>
<ix:attr name="style" value="if(@manageable = 1, '', 'display:none')"/>
<div class="addPerm" style="padding:4px 0px 8px 2px">

<span class="addPerm">Add User<xf:action ev:event="click">
            		<xf:duplicate ref="." origin="instance('pal')/perm[@user]"/>
            	</xf:action></span>
| <span class="addPerm">Add Group<xf:action ev:event="click">
            		<xf:duplicate ref="." origin="instance('pal')/perm[@group]"/>
            	</xf:action></span>
<% if (useOrgs) { %>
| <span class="addPerm">Add Org. Position<xf:action ev:event="click">
            		<xf:duplicate ref="." origin="instance('pal')/perm[@relative]"/>
            	</xf:action></span>
<% } %>
</div>
   	    
   	    
 	<xf:submit submission="submission" class="diagBtn dbSave"/>
</div>
 		
 				
		</div>

    </div>

</form>