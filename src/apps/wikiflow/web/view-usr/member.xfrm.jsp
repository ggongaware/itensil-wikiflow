<%@ page import="itensil.security.web.UserUtil"%>

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
            <member xmlns="">
                <group/>
                <user/>
                <positions/>
            </member>
        </xf:instance>
        
        <xf:instance id="opos" src="../fil/home/org-positions.xml">
        	<org-positions xmlns="">
        		<position id="Manager" label="Manager"/>
        	</org-positions>
        </xf:instance>

		<xf:instance id="pal">
			<data xmlns="">
				<position id="" label=""/>
			</data>
		</xf:instance>	
		
		<xf:bind nodeset="member/position" type="xsd:NMTOKENS"/>
		<xf:bind nodeset="instance('opos')/position/@id" type="xsd:NMTOKEN"/>
		
        <xf:submission id="saveMem" replace="none" method="post" action="../uspace/setMember">
            <xf:action ev:event="xforms-submit-done">
                <xf:message level="ephemeral">Member info saved.</xf:message>
			</xf:action>
			<xf:action ev:event="xforms-submit-error">
				<xf:message level="modal">Problem saving...</xf:message>
			</xf:action>
		</xf:submission>

		<xf:submission id="saveOpos" replace="none" method="put" instance="opos">
			<xf:action ev:event="xforms-submit-done">
                <xf:message level="ephemeral">Positions saved.</xf:message>
                <xf:close/>
			</xf:action>
			<xf:action ev:event="xforms-submit-error">
				<xf:message level="modal">Problem saving...</xf:message>
			</xf:action>
		</xf:submission>
		
    </xf:model>

    
    <div style="padding:6px;width:350px">
		
		<xf:select ref="positions">
			<xf:label>Positions</xf:label>
			<xf:itemset nodeset="instance('opos')/position">
				<xf:label ref="@label"/>
				<xf:value ref="@id"/>
			</xf:itemset>	
		</xf:select>
		<p>
		<xf:submit submission="saveMem" class="diagBtn dbSave"/>
		</p>
		<% if (UserUtil.isAdmin(request)) { %>
		<div style="margin-top:16px;font-size:12px"><u class="link">View/Edit Positions &gt;</u>
			<ix:dialog title="Person Details" ev:event="click" style="width:380px">
			 	<div style="padding:6px;width:350px">
			  	<table class="attrOpt">
			  	<tr>
			  		<th>Label</th>
			  		<th>Id</th>
			  		<th/>
			  	</tr>
			  	<xf:repeat nodeset="instance('opos')/position">
			  	<tr>
			  		<td><xf:input style="width:18em" ref="@label"/></td>
			  		<td><xf:input style="width:7em" ref="@id">
			  			<xf:hint>Avoid spaces in the id.</xf:hint>
			  		</xf:input></td>
			  		<td class="attrDel"><div class="attrDel" title="Remove">X<xf:destroy ref="." ev:event="click"/></div></td>
				</tr>
			  	</xf:repeat>
		 		<tr>
		   			<td colspan="3"><u class="attrLink"><xf:duplicate ref="instance('opos')" ev:event="click"
		  				origin="instance('pal')/position"/>Add Position</u></td>
		     	</tr>
			  	</table>
			  	<p>
				<xf:submit submission="saveOpos" class="diagBtn dbSave">
					<xf:close ev:event="DOMActivate"/>
				</xf:submit>
				</p>
				</div>
			</ix:dialog>
 	</div>
	    <% } %>
		


    </div>
    
    
    

</form>
