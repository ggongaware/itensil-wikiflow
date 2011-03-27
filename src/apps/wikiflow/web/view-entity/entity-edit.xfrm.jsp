<%@ page import="itensil.security.User" contentType="text/xml" %>
<form xmlns="http://www.w3.org/2002/06/xhtml2"
    xmlns:xf="http://www.w3.org/2002/xforms"
    xmlns:exf="http://www.exforms.org/exf/1-0"
    xmlns:ev="http://www.w3.org/2001/xml-events"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
    xmlns:ix="http://itensil.com/ns/xforms"
    xmlns:rl="http://itensil.com/ns/rules"
    xmlns:ie="http://itensil.com/ns/entity">

<xf:model id="emodel">
	<xf:instance id="mod" src="">
      	<entity xmlns="http://itensil.com/ns/entity">
			<data dataRev="0" browseRev="0"/>
			<forms/>
			<queries/>
			<events>
				<event-sys type="create" relation=""/>
				<event-sys type="delete" relation=""/>
			</events>
		</entity>
   	</xf:instance>
   	
   	<xf:instance id="entIns" src="../entity/listEntities?uri=/home/entity"/>
   	
   	<xf:instance id="pal">
     	<pal xmlns="http://itensil.com/ns/entity">
     	
	      	<attr name="" label="" browse="" default="" type="xsd:string"/>
	      	<item label="Label" value="value"/>
			<entity name="" type="" relation="1orMore"/>
			<action type="flow" owner="" flow=""/>
			<action type="ruleset" owner="" rulesrc="" rule=""/>
			<ruleset src="" rule="" label=""/>
			<event type="" />
			<event-sys type="" relation=""/>
			<form src="" label=""/>
   		</pal>
 	</xf:instance>
 	
 	<xf:instance id="formGen">
 		<data xmlns="http://itensil.com/ns/entity" name="MyForm"/>
 	</xf:instance>
   	
   	<xf:bind nodeset="queries/ruleset/@src" type="ix:file"/>
   	<xf:bind nodeset="forms/form/@src" type="ix:file"/>
   	<xf:bind nodeset="events/event/@type" type="ix:fileName" />
   	
   	<%-- <xf:bind nodeset="event//action/@rulesrc" type="ix:file"/> --%>
   	<xf:bind nodeset="events//action/@flow" type="ix:file"/>
   	<xf:bind nodeset="data//attr/@name" type="ix:varName"/>
   	<xf:bind nodeset="data//attr/@browse" relevant="not(starts-with(../@type,'ix:composite'))" />
   	<xf:bind nodeset="data/entity/@name" type="ix:varName"/>
   	<xf:bind nodeset="data//item/@value" type="xsd:NMTOKEN"/>
   
   	
   	<xf:submission id="save" replace="none" method="put" action="" validate="false">
         <xf:action ev:event="xforms-submit-done">
          	<xf:message level="ephemeral">Entity saved.</xf:message>
		</xf:action>
		<xf:action ev:event="xforms-submit-error">
			<xf:message level="ephemeral">Problem saving...</xf:message>
		</xf:action>
	</xf:submission>
      
   	
</xf:model>


<ix:template name="_dataAttr">
 	<td>
 	<exf:variable name="pathSize" value="count(ancestor::attr)"/>
 	<exf:variable name="path" value="join(ancestor::attr/@name, '/')"/>
 	<xf:input ref="@name" style="width:12em">
 		<xf:label><ix:attr name="style" value="concat('overflow:hidden;color:#666;text-align:right;padding-right:3px;font-size:10px;width:', $pathSize * 72, 'px')"/>
 		<xf:output value="$path"/></xf:label>
 		<xf:action ev:event="xforms-value-changed">
				<xf:setvalue ref="../@label" value="../@name" exf:if=". = ''"/>
				<xf:setvalue ref="../../@dataRev" value=". + 1"/>
			</xf:action>
 	</xf:input>
 	</td>
 	<td>
 		<xf:select1 ref="@type" style="width:9em;font-size:11px">
            <xf:label/>
            <xf:item><xf:label>Text</xf:label><xf:value>xsd:string</xf:value></xf:item>
            <xf:item><xf:label>Text&#160;[array]</xf:label><xf:value>xsd:string:array</xf:value></xf:item>
            <xf:item><xf:label>Number</xf:label><xf:value>xsd:float</xf:value></xf:item>
            <xf:item><xf:label>Number&#160;[array]</xf:label><xf:value>xsd:float:array</xf:value></xf:item>
            <xf:item><xf:label>Dollars&#160;($)</xf:label><xf:value>ix:currencyUSD</xf:value></xf:item>
            <xf:item><xf:label>Dollars&#160;[array]&#160;($)</xf:label><xf:value>ix:currencyUSD:array</xf:value></xf:item>
            <xf:item><xf:label>Percent&#160;(%)</xf:label><xf:value>ix:percent</xf:value></xf:item>
            <xf:item><xf:label>Percent&#160;[array]&#160;(%)</xf:label><xf:value>ix:percent:array</xf:value></xf:item>
            <xf:item><xf:label>Select&#160;List</xf:label><xf:value>xsd:NMTOKEN</xf:value></xf:item>
            <xf:item><xf:label>Select&#160;List&#160;[array]</xf:label><xf:value>xsd:NMTOKEN:array</xf:value></xf:item>
            <xf:item><xf:label>Multi-Select</xf:label><xf:value>xsd:NMTOKENS</xf:value></xf:item>
            <xf:item><xf:label>Yes/No</xf:label><xf:value>xsd:boolean</xf:value></xf:item>
            <xf:item><xf:label>Date</xf:label><xf:value>xsd:date</xf:value></xf:item>
            <xf:item><xf:label>Date&#160;&amp;&#160;Time</xf:label><xf:value>xsd:dateTime</xf:value></xf:item>
            <xf:item><xf:label>Email</xf:label><xf:value>ix:email</xf:value></xf:item>
            <xf:item><xf:label>File</xf:label><xf:value>ix:file</xf:value></xf:item>
            <xf:item><xf:label>User</xf:label><xf:value>ix:user</xf:value></xf:item>
            <xf:item><xf:label>User&#160;Group</xf:label><xf:value>ix:userGroup</xf:value></xf:item>
            <xf:item><xf:label>Web&#160;Link</xf:label><xf:value>ix:http</xf:value></xf:item>
            <xf:item><xf:label>Composite&#160;&gt;</xf:label><xf:value>ix:composite</xf:value></xf:item>
            <xf:item><xf:label>Composite&#160;&gt;&#160;[array]</xf:label><xf:value>ix:composite:array</xf:value></xf:item>
        </xf:select1>
        <div>
        <u style="cursor:pointer" title="Edit list options">Options<script ev:event="click">
        		if (model.getValue("count(item)", contextNode) == 0) {
        			model.duplicateNode("instance('pal')/item", ".", null, contextNode);
        		}
        		var diag = xfTemplateDialog("Select Attribute: " + model.getValue("@name",contextNode), true, 
        			document.body, model.getForm(), "attrSelectOpts", contextNode, false, null, App.chromeHelp);
        		diag.show(220, 330);
         </script></u><ix:attr name="style" value="if(contains('xsd:NMTOKENS xsd:NMTOKEN:array', @type),'','display:none')"/></div>
 	</td>
 	<td>
 	<div><ix:attr name="style" value="if(starts-with(@type,'ix:composite'),'display:none','')"/>
 	<xf:input ref="@default" style="width:7em">
 		<xf:label/>
 		<xf:action ev:event="xforms-value-changed">
				<xf:setvalue ref="../../@dataRev" value=". + 1"/>
			</xf:action>
 	</xf:input>
 	</div>
 	<div><ix:attr name="style" value="if(starts-with(@type,'ix:composite'),'padding-top:2px','display:none')"/>
 	<u class="attrLink" title="Add field inside composite" stopdrag="true"><xf:action ev:event="click">
            	<xf:duplicate ref="." origin="instance('pal')/attr"/>
            </xf:action>Add sub-field...</u></div>
 	</td>
 	<td>
 	<xf:input ref="@label" style="width:12em"><xf:label/></xf:input>
 	</td>
 	<td>
 	<xf:select1 ref="@browse" style="width:5em">
      	<xf:label/>
      	<xf:item><xf:label>Hide</xf:label><xf:value/></xf:item>
       	<xf:item><xf:label>Column 1</xf:label><xf:value>1</xf:value></xf:item>
       	<xf:item><xf:label>Column 2</xf:label><xf:value>2</xf:value></xf:item>
       	<xf:item><xf:label>Column 3</xf:label><xf:value>3</xf:value></xf:item>
       	<xf:item><xf:label>Column 4</xf:label><xf:value>4</xf:value></xf:item>
       	<xf:item><xf:label>Column 5</xf:label><xf:value>5</xf:value></xf:item>
       	<xf:item><xf:label>Column 6</xf:label><xf:value>6</xf:value></xf:item>
       	<xf:item><xf:label>Column 7</xf:label><xf:value>7</xf:value></xf:item>
       	<xf:item><xf:label>Column 8</xf:label><xf:value>8</xf:value></xf:item>
       	<xf:item><xf:label>Column 9</xf:label><xf:value>9</xf:value></xf:item>
       	<xf:item><xf:label>Column 10</xf:label><xf:value>10</xf:value></xf:item>
       	<xf:item><xf:label>Column 11</xf:label><xf:value>11</xf:value></xf:item>
       	<xf:item><xf:label>Column 12</xf:label><xf:value>12</xf:value></xf:item>
       	<xf:setvalue ev:event="xforms-value-changed" ref="../../@browseRev" value=". + 1"/>
     </xf:select1>
 	</td>
 	<td>&#160;</td>
 	<td class="attrDel"><div class="attrDel" title="Remove">X
 	<xf:action ev:event="click">
 		<xf:setvalue ref="../@dataRev" value=". + 1"/>
 		<xf:destroy ref="."/>
 	</xf:action></div></td>
</ix:template>

<div class="entData">
<div class="minorHead">Fields</div>
<div class="minorTreeBox">
 	<table class="entManager">
 	<thead>
   	<tr>
   		<th>Name</th>
   		<th>Type</th>
   		<th>Default Value</th>
   		<th>Display Label</th>
   		<th>Browsing</th>
   		<th/>
   		<th/>
   	</tr>
   	</thead>
   	<tbody>
   	<xf:repeat nodeset="data//attr[parent::attr or parent::data]">
   	 <tr valign="top">
   		<ix:include template="_dataAttr" nodiv="1"/>
   	 </tr>
   	</xf:repeat>
   	</tbody>
   	<tfoot>
   	<tr>
   		<td colspan="6" class="addRow"><u class="attrLink"><xf:duplicate ref="data" ev:event="click"
   			origin="instance('pal')/attr" before="entity"/>Add Field</u></td>
  	</tr>
  	</tfoot>
   </table>
</div>
<p/>

<div class="minorHead">Entity Relations</div>
<div class="minorTreeBox">
		<table class="entManager">
		<thead>
		<tr>
	   		<th>Relationship</th>
	   		<th>Type</th>
	   		<th>Name</th>
	   		<th>Browse</th>
	   		<th/>
	   		<th/>
	   		<th/>
	   		<th/>
	   	</tr>
	   	</thead>
 		<xf:repeat nodeset="data/entity">
 		<tbody>
 			<tr valign="top" class="entRow">
 			<td>
 			<xf:select1 ref="@relation" style="font-size:10px">
 				<xf:label>Has</xf:label>
              <xf:item><xf:label>One&#160;or&#160;More</xf:label><xf:value>1orMore</xf:value></xf:item>
              <xf:item><xf:label>One</xf:label><xf:value>1</xf:value></xf:item>
 			</xf:select1>
 			</td>
 			<td>
 			<xf:select1 ref="@type" style="font-size:10px">
 				<xf:label/>
              	<xf:item><xf:label>-Select&#160;Entity-</xf:label><xf:value/></xf:item>
              	<xf:itemset nodeset="instance('entIns')/node">
              		<xf:label ref="@uri"/>
              		<xf:value ref="@id"/>
              	</xf:itemset>
              	<xf:setvalue ev:event="xforms-value-changed" ref="../@name" 
              		value="instance('entIns')/node[current()/../@type = @id]/@uri" exf:if=". = ''"/>
 			</xf:select1>
 			</td>
 			<td>
 			<xf:input style="width:9em" ref="@name">
	  			<xf:label style="width:55px;">Called</xf:label>
	  			<xf:setvalue ev:event="xforms-value-changed" ref="../@dataRev" value=". + 1"/>
	  		</xf:input>
 			</td>
 			<td>
		 	<xf:select1 ref="@browse" style="width:5em">
		      	<xf:label/>
		      	<xf:item><xf:label>Hide</xf:label><xf:value/></xf:item>
		       	<xf:item><xf:label>Column 1</xf:label><xf:value>1</xf:value></xf:item>
		       	<xf:item><xf:label>Column 2</xf:label><xf:value>2</xf:value></xf:item>
		       	<xf:item><xf:label>Column 3</xf:label><xf:value>3</xf:value></xf:item>
		       	<xf:item><xf:label>Column 4</xf:label><xf:value>4</xf:value></xf:item>
		       	<xf:item><xf:label>Column 5</xf:label><xf:value>5</xf:value></xf:item>
		       	<xf:item><xf:label>Column 6</xf:label><xf:value>6</xf:value></xf:item>
		       	<xf:item><xf:label>Column 7</xf:label><xf:value>7</xf:value></xf:item>
		       	<xf:item><xf:label>Column 8</xf:label><xf:value>8</xf:value></xf:item>
		       	<xf:item><xf:label>Column 9</xf:label><xf:value>9</xf:value></xf:item>
		       	<xf:item><xf:label>Column 10</xf:label><xf:value>10</xf:value></xf:item>
		       	<xf:item><xf:label>Column 11</xf:label><xf:value>11</xf:value></xf:item>
		       	<xf:item><xf:label>Column 12</xf:label><xf:value>12</xf:value></xf:item>
		       	<xf:setvalue ev:event="xforms-value-changed" ref="../../@browseRev" value=". + 1"/>
		     </xf:select1>
		 	</td>
		 	<td style="padding:4px 10px 2px 10px"><u class="attrLink" title="Add relationship attribute" stopdrag="true"><xf:action ev:event="click">
            	<xf:duplicate ref="." origin="instance('pal')/attr"/>
            </xf:action>Add attribute...</u></td>
 			<td>&#160;</td>
 			<td>&#160;</td>
 			<td class="attrDel"><div class="attrDel" title="Remove">X<xf:action ev:event="click">
   				<xf:setvalue ref="../@dataRev" value=". + 1"/>
   				<xf:destroy ref="."/>
   			</xf:action></div></td>
 		</tr>
 		<xf:repeat nodeset="attr">
        <tr valign="top" class="entRow">
         	<td>&#160;</td>
         	<td>&#160;</td>
         	<td>
   			<xf:input ref="@name" style="width:9em;font-size:11px;border:1px solid #999">
   				<xf:label style="width:55px;">Attribute</xf:label>
   				<xf:setvalue ev:event="xforms-value-changed" ref="../../../@dataRev" value=". + 1"/>
   			</xf:input>
   			</td>
   			<td>
   				<xf:select1 ref="@type" style="width:6em;font-size:10px">
                   <xf:label/>
                   <xf:item><xf:label>Text</xf:label><xf:value>xsd:string</xf:value></xf:item>
                   <xf:item><xf:label>Number</xf:label><xf:value>xsd:float</xf:value></xf:item>
                   <xf:item><xf:label>Dollars&#160;($)</xf:label><xf:value>ix:currencyUSD</xf:value></xf:item>
                   <xf:item><xf:label>Percent&#160;(%)</xf:label><xf:value>ix:percent</xf:value></xf:item>
                   <xf:item><xf:label>Select&#160;List</xf:label><xf:value>xsd:NMTOKEN</xf:value></xf:item>
                   <xf:item><xf:label>Multi-Select</xf:label><xf:value>xsd:NMTOKENS</xf:value></xf:item>
                   <xf:item><xf:label>Yes/No</xf:label><xf:value>xsd:boolean</xf:value></xf:item>
                   <xf:item><xf:label>Date</xf:label><xf:value>xsd:date</xf:value></xf:item>
                   <xf:item><xf:label>Date&#160;&amp;&#160;Time</xf:label><xf:value>xsd:dateTime</xf:value></xf:item>
                   <xf:item><xf:label>Email</xf:label><xf:value>ix:email</xf:value></xf:item>
                   <xf:item><xf:label>File</xf:label><xf:value>ix:file</xf:value></xf:item>
                   <xf:item><xf:label>User</xf:label><xf:value>ix:user</xf:value></xf:item>
           	 	  <!--  <xf:item><xf:label>User&#160;Group</xf:label><xf:value>ix:userGroup</xf:value></xf:item> -->
                   <xf:item><xf:label>Web&#160;Link</xf:label><xf:value>ix:http</xf:value></xf:item>
               </xf:select1>
               <div>
               <u style="cursor:pointer" title="Edit list options">Options<script ev:event="click">
               		if (model.getValue("count(item)", contextNode) == 0) {
               			model.duplicateNode("instance('pal')/item", ".", null, contextNode);
               		}
               		var diag = xfTemplateDialog("Select Attribute: " + model.getValue("@name",contextNode), true, 
               			document.body, model.getForm(), "attrSelectOpts", contextNode, false, null, App.chromeHelp);
               		diag.show(220, 330);
                </script></u><ix:attr name="style" value="if(contains('xsd:NMTOKENS xsd:NMTOKEN:array', @type),'','display:none')"/></div>
   			</td>
   			<td colspan="2"><xf:input ref="@default" style="width:5em">
   				<xf:label>Default</xf:label>
   				<xf:action ev:event="xforms-value-changed">
	   				<xf:setvalue ref="../../../@dataRev" value=". + 1"/>
	   			</xf:action>
   			</xf:input></td>
         	<td>&#160;</td>
 			<td class="attrDel"><div class="attrDel" title="Remove">X<xf:action ev:event="click">
   				<xf:setvalue ref="../../@dataRev" value=". + 1"/>
   				<xf:destroy ref="."/>
   			</xf:action></div></td>
 		 </tr>
 		</xf:repeat>
 		<tr>
 			<td colspan="8">&#160;</td>
 		</tr>
 		</tbody>
 		</xf:repeat>
 		<tfoot>
 		<tr>
 		<td colspan="8" class="addRow"><u class="attrLink"><xf:action ev:event="click">
 			<xf:duplicate ref="data" origin="instance('pal')/entity"/>
 			</xf:action>Add Relationship</u></td>
		</tr>
		</tfoot>
 		</table>
</div>
</div>

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
  			<xf:hint>Avoid spaces in the value. For [array] use a numeric value.</xf:hint>
  		</xf:input></td>
  		<td class="attrDel"><div class="attrDel" title="Remove">X<xf:destroy ref="." ev:event="click"/></div></td>
	</tr>
  	</xf:repeat>
 		<tr>
   			<td colspan="3"><u class="attrLink"><xf:duplicate ref="." ev:event="click"
  			origin="instance('pal')/rl:item"/>Add Option</u></td>
     	</tr>
  	</table>
 	</div>
</ix:template>


<ix:template name="formManager" class="attrManager">
   	<div style="margin-top:10px">
   		<table class="entManager">
   		<tr>
	  		<th>Form</th>
	  		<th>Label</th>
	  		<th/>
	  		<th/>
	  		<th/>
	  	</tr>
   		<xf:repeat nodeset="forms/form">
   		<tr valign="top">
   			<td style="padding:2px 12px 4px 4px">
   				<xf:input ref="@src" extensions="xfrm" note="Drop a form here">
   					<xf:setvalue ev:event="xforms-value-changed" ref="../@label" 
   						value="substring-before(script('Uri.name(Xml.stringForNode(contextNode))',../@src),'.xfrm')" exf:if=". = ''"/>
   				</xf:input>
   			</td>
   			<td>
   			<xf:input ref="@label" style="width:8em"><xf:label/></xf:input>
   			</td>
   			<td>&#160;</td>
   			<td class="attrDel"><div class="attrDel" title="Remove">X<xf:destroy ref="." ev:event="click"/></div></td>
   			<td>&#160;</td>
   		</tr>
   		</xf:repeat>
   		<tr>
   		<td colspan="5" class="addRow"><u class="attrLink"><xf:action ev:event="click">
 			<xf:duplicate ref="forms" origin="instance('pal')/form"/>
 			</xf:action>Add Existing Form</u></td>
  		</tr>
  		<tr>
   		<td colspan="5" class="addRow"><u class="attrLink"><xf:action ev:event="click">
				<script>
 				EntityCanvas.live.saveNewXform();
 				</script>
 			</xf:action>Add New Form</u></td>
  		</tr>
   		</table>
   	</div>
</ix:template>

<ix:template name="queryManager" class="attrManager">
   	<div style="margin-top:10px">
   	<table class="entManager">
   		<tr>
	  		<th>Query Rule</th>
	  		<th>Sub Rule</th>
	  		<th>Label</th>
	  		<th/>
	  		<th/>
	  	</tr>
   		<xf:repeat nodeset="queries/ruleset">
   		<tr valign="top">
   			<td style="padding:2px 12px 4px 4px">
   				<xf:input ref="@src" extensions="rule" note="Drop a rule here">
   					<xf:action ev:event="xforms-value-changed">
   						<xf:setvalue ref="../@label" 
   							value="substring-before(script('Uri.name(Xml.stringForNode(contextNode))',../@src),'.rule')" 
   							exf:if=". = ''"/>
   						<xf:rebuild model="emodel"/>
   					</xf:action>
   				</xf:input>
   			</td>
   			<td>
   			<exf:variable name="rulRoot" value="document(@src)/rl:ruleset"/>
			<xf:select1 ref="@rule" style="width:8em;font-size:10px">
		        <xf:label/>
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
   			</td>
   			<td>
   			<xf:input ref="@label" style="width:8em"><xf:label/></xf:input>
   			</td>
   			<td>&#160;</td>
   			<td class="attrDel"><div class="attrDel" title="Remove">X<xf:destroy ref="." ev:event="click"/></div></td>
   		</tr>
   		</xf:repeat>
   		<tr>
   		<td colspan="5" class="addRow"><u class="attrLink"><xf:action ev:event="click">
 			<xf:duplicate ref="queries" origin="instance('pal')/ruleset"/>
 			</xf:action>Add Existing Query</u></td>
  		</tr>
  		<tr>
   		<td colspan="5" class="addRow"><u class="attrLink"><xf:action ev:event="click">
				<script>
 				EntityCanvas.live.saveNewRuleset();
 				</script>
 			</xf:action>Add New Query</u></td>
  		</tr>
   		</table>
   	</div>
</ix:template>

<ix:template name="eventManager" class="attrManager">
   	<div>
<%--   	
   		<div class="minorHead">System Events</div>
		<div class="minorTreeBox">
		<table class="entManager">
   		<tr>
	  		<th>Event Type</th>
	  		<th/>
	  		<th/>
	  	</tr>
   		<xf:repeat nodeset="events/event-sys">
   		<tr valign="top">
   			<td style="padding:2px 12px 4px 4px;font-size:12px"><xf:output ref="@type"/></td>
   			<td>&#160;</td>
   			<td><u class="attrLink">Add Action...</u></td>
   		</tr>
   		</xf:repeat>
   		</table>
		</div>
--%>		
		
		<div class="minorHead">User Events</div>
		<div class="minorTreeBox">
		<table class="entManager">
		<thead>
   		<tr>
	  		<th colspan="2">Event Type</th>
	  		<th/>
	  		<th/>
	  		<th/>
	  		<th/>
	  		<th/>
	  	</tr>
	  	</thead>
   		<xf:repeat nodeset="events/event">
   		<tbody>
   		<tr class="entRow">
   			<td colspan="2" style="padding:2px 12px 4px 4px">
   				<xf:input ref="@type"></xf:input>
   			</td>
   			<td>&#160;</td>
   			<td><u class="attrLink"><xf:action ev:event="click">
 				<xf:duplicate ref="." origin="instance('pal')/action[@type='flow']"/>
 			</xf:action>Add Action...</u></td>
 			<td>&#160;</td>
   			<td class="attrDel"><div class="attrDel" title="Remove">X<xf:destroy ref="." ev:event="click"/></div></td>
   			<td>&#160;</td>
   		</tr>
   		<xf:repeat nodeset="action">
   		<tr class="entRow">
   		<%-- TODO: support other action types --%>
   			<td>&gt; Launch:</td>
   			<td>   				
   			<xf:input ref="@flow" display="parent" extensions="flow" note="Drop a process chart here"></xf:input>
   			</td>
   			<td colspan="3">&#160;</td>
   			<td class="attrDel"><div class="attrDel" title="Remove">X<xf:destroy ref="." ev:event="click"/></div></td>
   			<td>&#160;</td>
   		</tr>
   		</xf:repeat>
   		<tr>
   			<td colspan="7">&#160;</td>
   		</tr>
   		</tbody>
   		</xf:repeat>
   		<tfoot>
   		<tr>
   		<td colspan="6" class="addRow"><u class="attrLink"><xf:action ev:event="click">
 			<xf:duplicate ref="events" origin="instance('pal')/event"/>
 			</xf:action>Add User Event</u></td>
  		</tr>
  		</tfoot>
   		</table>
		</div>
		
   	</div>
</ix:template>

<ix:template name="formGenOptions">
	<div style="position:relative;width:440px;height:350px;">
 	
 			<table style="position:absolute;top:10px;left:10px">
 				<tr>
 					<td>
 					<xf:input ref="instance('formGen')/@name">
 						<xf:label>Form file: </xf:label>
 					</xf:input>
 					</td>
 					<td>.xfrm</td>
 				</tr>
 			</table>

			<div style="position:absolute;top:50px;left:10px">
			Display Fields:
			<div style="width:200px;height:200px;padding:4px;border:1px solid #999;overflow:auto">
				<xf:repeat nodeset="instance('formGen')/*">
					<div style="background-color:#fff;padding:4px;margin:2px;">
					<table>
					<tr>
						<td><xf:output ref="@name"/></td>
						<%--
						<td>
						<xf:select1 ref="@mode">
							<xf:item><xf:label>Input</xf:label><xf:value></xf:value></xf:item>
							<xf:item><xf:label>Output</xf:label><xf:value>output</xf:value></xf:item>
						</xf:select1>
						</td>
						--%>
						<td>
						<ix:attr name="style" value=" if(@type='xsd:string' or @type='xsd:NMTOKEN','','visibility:hidden')"/>
						<xf:select1 ref="@appearance">
							<xf:item><xf:label>Normal</xf:label><xf:value></xf:value></xf:item>
							<xf:item><xf:label>Full</xf:label><xf:value>full</xf:value></xf:item>
						</xf:select1>
						</td>
						
						<td><b style="color:#900;cursor:pointer">&#160;x<xf:destroy ref="." ev:event="click"/></b></td>
					</tr>
					</table>
					</div>
				</xf:repeat>
			</div>
			</div>
			
			<div style="position:absolute;top:50px;left:240px">
			Available Fields:
			<div style="width:160px;height:200px;padding:4px;border:1px solid #999;overflow:auto">
			<xf:repeat nodeset="data/attr|data/entity">
				<div style="background-color:#fff;padding:4px;margin:2px;cursor:pointer">
				<xf:duplicate ref="instance('formGen')" origin="." ev:event="click"/>
				<b style="color:#009">&lt;&#160;</b>
				<xf:output ref="@name"/></div>
			</xf:repeat>
			</div>
			</div>
 	
 		<table class="buttons" style="position:absolute;top:290px;left:10px">
 			<tr>
 				<td><xf:trigger class="diagBtn dbOk">
 					<xf:action ev:event="DOMActivate">
 						<script>
 						
 						var xfMod = model;
 						var name = xfMod.getValue("instance('formGen')/@name");
 						if (name) {
							var doc = xfMod.getInstanceDocument('formGen');
							
							var dat = doc.documentElement;
							if (!dat) return;
							
							var xfDoc = Data.xformFromAttrs(dat, "../view-entity/entity-template.xfrm");
							var dstUri = Uri.absolute(xfMod.getForm().__defPath, name) + ".xfrm";
							Data.saveDoc(xfDoc, "../fil" + dstUri, function(resDoc, uri) {
									var dup = xfMod.duplicateNode("instance('pal')/form", "forms");
									xfMod.setValue("@src", name + ".xfrm", dup);
									xfMod.setValue("@label", name, dup);
									xfMod.rebuild();
								});
							doc = null; dat = null; xfDoc = null; contextNode = null;
						}
			
 						</script>
 						<xf:close/>
 					</xf:action>
 				</xf:trigger></td>
 				<td><xf:trigger class="diagBtn dbCancel">
 					<xf:close ev:event="DOMActivate"/>
 				</xf:trigger></td>
 			</tr>
 		</table>
 		
    </div>
</ix:template>

</form>