<%@ page import="itensil.security.User" contentType="text/xml" %>
<% 	User user = (User)request.getUserPrincipal();
	boolean useEntities = false;
	if (user != null) {
		useEntities = user.getUserSpace().getFeatures().contains("entity");
	}
%>

<form xmlns="http://www.w3.org/2002/06/xhtml2"
    xmlns:xf="http://www.w3.org/2002/xforms"
    xmlns:exf="http://www.exforms.org/exf/1-0"
    xmlns:ev="http://www.w3.org/2001/xml-events"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
    xmlns:ix="http://itensil.com/ns/xforms"
    xmlns:rl="http://itensil.com/ns/rules">
    
    
<xf:model id="rmodel">
  
     <xf:instance id="mod" src="">
      	<ruleset xmlns="http://itensil.com/ns/rules">
      		<data>
      			<attr name="mynum" type="xsd:float"/>
      		</data>

      		<returns>
      			<return id="pass"/>
	       		<return id="fail"/>
	       	</returns>

	      	<rule id="default">
	       		<when type="xpath" test="mynum &gt; 2">
	       			<return id="pass"/>
	       		</when>
	       		<otherwise>
	       			<return id="fail"/>
	       		</otherwise>
	       	</rule>
	       	
	       	<rule id="my sub">
	       		<when type="xpath" test="mynum &lt; 20">
	       			<return id="fail"/>
	       		</when>
	       		<otherwise>
	       			<return id="pass"/>
	       		</otherwise>
	       	</rule>
       	
	       	<testdata xmlns="">
	       		<mynum>3</mynum>
	       	</testdata>
      	</ruleset>
     </xf:instance>

	 <% if (useEntities) { %>
     <xf:instance id="entIns" src="../entity/listEntities?uri=/home/entity"/>
     <% } %>
     
     <xf:instance id="pal">
     	<pal xmlns="http://itensil.com/ns/rules">
	      	<when type="xpath" test=""/>
	      	<when type="" field="" op="=" arg="" aggregate=""/>
	      	<otherwise/>
	      	<sub id=""/>
	      	<rule id=""/>
	      	<set type="" field="" value=""/>
	      	<return id=""/>
	      	<attr name="" type="xsd:float"/>
	      	<item label="Label" value="value"/>
	      	<find entity="" type="" field="" op="=" arg="" aggregate=""/>
	      	<and type="" field="" op="=" arg="" aggregate=""/>
   		</pal>
 	</xf:instance>
   
    <xf:bind nodeset="data/attr/@name" type="ix:varName"/>
    <xf:bind nodeset="rule/when[@type='ix:percent']/@arg" type="ix:percent"/>

   	<xf:submission id="save" replace="none" method="put" action="" validate="false">
         <xf:action ev:event="xforms-submit-done">
          	<xf:message level="ephemeral">Rule saved.</xf:message>
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
 	<xf:output value="$path"  style="color:#666;font-size:10px;"/>
 	<div><ix:attr name="style" value="concat('padding-left:', $pathSize * 3, 'px')"/>
 	<xf:input ref="@name" style="width:7em;font-size:11px;border:1px solid #999">
 		<xf:label/>
 	</xf:input>
 	</div>
 	</td>
   	<td>
   		<xf:select1 ref="@type" style="width:8em;font-size:11px">
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
         <div><ix:attr name="style" value="if(starts-with(@type,'ix:composite'),'padding-top:2px','display:none')"/>
	 	<u class="attrLink" title="Add field inside composite" stopdrag="true"><xf:action ev:event="click">
	            	<xf:duplicate ref="." origin="instance('pal')/attr"/>
	            </xf:action>Add sub-field...</u></div>
 	</td>

 	<td class="attrDel"><div class="attrDel" title="Remove">X<xf:destroy ref="." ev:event="click"/></div></td>
  	<td>
  		<div class="attrDrag mb_inpuIco" title="Drag Field">&#160;<script>
  			var dnd = dndGetCanvas(document.body);
  			uiParent._ctxNode = App.disposableNode(contextNode);
  			dnd.makeDraggable(uiParent, "dndRuleSrcTr");
  		</script><ix:attr name="style" value="if(@name='' or starts-with(@type,'ix:composite'),'display:none','')"/></div>
  	</td>
</ix:template>

<ix:template name="attrManager" class="attrManager">
   	<script>
   		var dnd = dndGetCanvas(document.body);
   		dnd.addDNDType(new RuleSrcTreeDNDType());
   	</script>
   	<table class="attrManager">
   	<thead>
   	<tr>
   		<th>Name</th>
   		<th>Type</th>
   		<th/>
   		<th/>
   	</tr>
   	</thead>
   	<tbody>
   	<xf:repeat nodeset="rl:data//rl:attr">
   	 <tr valign="top">
   		<ix:include template="_dataAttr" nodiv="1"/>
   	 </tr>
   	</xf:repeat>
   	</tbody>
   	<tfoot>
   	<tr>
   		<td colspan="4"><u class="attrLink"><xf:duplicate ref="rl:data" ev:event="click"
   			origin="instance('pal')/rl:attr"/>Add Field</u></td>
  	</tr>
  	</tfoot>
   </table>
   	
   	
   	<div style="margin-top:10px">
   		<table class="attrManager">
   		<tr>
		<td>Otherwise...</td>
		<td>
   		<div class="attrDrag mb_inpuIco" title="Drag Test">&#160;<script>
   				var dnd = dndGetCanvas(document.body);
   				uiParent._emptyType = "otherwise";
   				dnd.makeDraggable(uiParent, "dndRuleSrcTr");
   			</script></div>
   			</td>
   		</tr>
		<tr>
		<td>Custom Expression (XPath)</td>
		<td>
   		<div class="attrDrag mb_inpuIco" title="Drag Test">&#160;<script>
   				var dnd = dndGetCanvas(document.body);
   				uiParent._emptyType = "xpath";
   				dnd.makeDraggable(uiParent, "dndRuleSrcTr");
   			</script></div>
   			</td>
   		</tr>
   		<% if (useEntities) { %>
   		<tr>
		<td>Find Entity</td>
		<td>
   		<div class="attrDrag mb_inpuIco" title="Drag Test">&#160;<script>
   				var dnd = dndGetCanvas(document.body);
   				uiParent._emptyType = "find";
   				dnd.makeDraggable(uiParent, "dndRuleSrcTr");
   			</script></div>
   			</td>
   		</tr>
   		<% } %>
   		</table>
   		
   		<p>
   		<u class="attrLink"><xf:action ev:event="click">
   				<script>
   				RuleCanvas.live.saveTestXform();
   				</script>
   			</xf:action>Save fields as form &gt;</u>
   		</p>
   	</div>

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


<ix:template name="returnManager" class="attrManager">
   	<div style="margin-top:10px">
   		<table class="attrManager">
   		<xf:repeat nodeset="rl:returns/rl:return">
   		<tr valign="top">
   			<td style="padding:4px 12px 2px 2px"><xf:output ref="@id" class="retLabel"/></td>
   			<td>&#160;</td>
   			<td class="attrDel"><div class="attrDel" title="Remove">X<xf:destroy ref="." ev:event="click"/></div></td>
   			<td>
   			<div class="attrDrag mb_inpuIco" title="Drag Attribute">&#160;<script>
   				var dnd = dndGetCanvas(document.body);
   				uiParent._ctxNode = App.disposableNode(contextNode);
   				dnd.makeDraggable(uiParent, "dndRuleSrcTr");
   			</script></div>
   			</td>
   		</tr>
   		</xf:repeat>
   		<tr>
   		<td colspan="4"><u class="attrLink"><script ev:event="click">
   				var diag = Dialog.prompt("Return ID:","return", function(uin) {
   							model.setValue("instance('pal')/rl:return/@id", uin);
   							model.duplicateNode("instance('pal')/rl:return", "rl:returns", null, contextNode);
   							model.rebuild();
   						});
   				diag.show(getMouseX(event.uiEvent) + 20, getMouseY(event.uiEvent) - 20);
   				</script>Add Return</u></td>
  		</tr>
   		</table>
   	</div>
</ix:template>


<ix:template name="subManager" class="attrManager">
   	<div style="margin-top:10px">
   		<table class="attrManager">
   		<xf:repeat nodeset="rl:rule[@id!='default']">
   		<tr valign="top">
   			<td style="padding:4px 10px 2px 2px"><xf:output ref="@id" class="retLabel"/></td>
   			<td style="padding:4px 10px 2px 2px"><u style="cursor:pointer" title="View sub rule"><script ev:event="click">
   				RuleCanvas.live.showRule(model.getValue("@id", contextNode));
   			</script>view &gt;</u></td>
   			<td>&#160;</td>
   			<td class="attrDel"><div class="attrDel" title="Remove">X<xf:destroy ref="." ev:event="click"/></div></td>
   			<td>
   			<div class="attrDrag mb_inpuIco" title="Drag Attribute">&#160;<script>
   				var dnd = dndGetCanvas(document.body);
   				uiParent._ctxNode = App.disposableNode(contextNode);
   				dnd.makeDraggable(uiParent, "dndRuleSrcTr");
   			</script></div>
   			</td>
   		</tr>
   		</xf:repeat>
   		<tr>
   		<td colspan="5"><u class="attrLink"><script ev:event="click">
   				var diag = Dialog.prompt("Rule ID:","rule", function(uin) {
   							model.setValue("instance('pal')/rl:rule/@id", uin);
   							model.duplicateNode("instance('pal')/rl:rule", ".", null, contextNode);
   							model.rebuild();
   						});
   				diag.show(getMouseX(event.uiEvent) + 20, getMouseY(event.uiEvent) - 20);
   				</script>Add Sub Rule</u></td>
  		</tr>
   		</table>
   		
   	</div>
</ix:template>


<ix:template name="when-xpath" class="xpath">
     <div class="cont">
     <div class="delete"><span title="Remove" stopdrag="true">X<xf:action ev:event="click"> <xf:destroy ref="."/> </xf:action></span></div>
     <table>
         <tr>
            <td style="vertical-align:top">If:</td>
            <td><pre style="font-size:11px;width:40em;margin:0"><xf:output ref="@test"/></pre></td>
            <td style="padding:2px 10px 2px 10px;vertical-align:top"><u style="cursor:pointer" title="Open XPath editor" stopdrag="true"><script ev:event="click">
   				RuleCanvas.live.xpEditor(event.uiEvent, App.disposableNode(contextNode));
   			</script>&lt; edit</u></td>
            <td style="vertical-align:top">
            	<div class="traceMsg"></div>
            </td>
         </tr>
     </table>
     
     </div>
</ix:template>
 
 
<ix:template name="return" class="return">
  	<div class="cont">
  	<div class="delete"><span title="Remove" stopdrag="true">X<xf:action ev:event="click"> <xf:destroy ref="."/> </xf:action></span></div>
  	<table>
         <tr>
            <td>Return:</td>
            <td><xf:output class="retLabel" ref="@id"/></td>
         </tr>
    </table>
    </div>
</ix:template>
 
 
<ix:template name="otherwise" class="otherwise">
  	<div class="cont">
  	<div class="delete"><span title="Remove" stopdrag="true">X<xf:action ev:event="click"> <xf:destroy ref="."/> </xf:action></span></div>
  	<table>
         <tr>
            <td>Otherwise...</td>
         </tr>
    </table>
    </div>
</ix:template>

<ix:template name="sub" class="sub">
  	<div class="cont">
  	<div class="delete"><span title="Remove" stopdrag="true">X<xf:action ev:event="click"> <xf:destroy ref="."/> </xf:action></span></div>
  	<table>
         <tr>
           	<td>Rule:</td>
            <td><xf:output ref="@id" class="retLabel"/></td>
            <td style="padding:2px 10px 2px 10px"><u style="cursor:pointer" title="View sub rule" stopdrag="true"><script ev:event="click">
   				RuleCanvas.live.showRule(model.getValue("@id", contextNode));
   			</script>view &gt;</u></td>
         </tr>
    </table>
    </div>
</ix:template>

<ix:template name="set" class="set">
	<div class="cont">
	<div class="delete"><span title="Remove" stopdrag="true">X<xf:action ev:event="click"> <xf:destroy ref="."/> </xf:action></span></div>
     <table>
         <tr>
            <td>Set:</td>
            <td class="field"><xf:output ref="@field"/></td>
            <td>to</td>
            <td>
                <xf:input ref="@value" style="width:8em;font-size:11px"  stopdrag="true">
                     <xf:label/>
                 </xf:input>
            </td>
            <td>
            	<div class="traceMsg"></div>
            </td>
         </tr>
     </table>
    </div>
</ix:template>


<ix:template name="find" class="field">
	<script>
    	var entId = model.getValue("@type", contextNode);
      	if (entId) model.setInstanceIdSrc(entId, "../entity/getModel?id=" + entId);
    </script>
	<div class="cont">
	<div class="delete"><span title="Remove" stopdrag="true">X<xf:action ev:event="click"> <xf:destroy ref="."/> </xf:action></span></div>
     <table>
     	<tbody>
         <tr>
            <td>If found:</td>
            <td><xf:select1 ref="@type">
   				<xf:label/>
                <xf:item><xf:label>-Select&#160;Entity-</xf:label><xf:value/></xf:item>
                <xf:itemset nodeset="instance('entIns')/node">
                	<xf:label ref="@uri"/>
                	<xf:value ref="@id"/>
                	
                </xf:itemset>
                <xf:action ev:event="xforms-value-changed">
               		<script>
               			var entId = model.getValue(".", contextNode);
               			if (entId) model.setInstanceIdSrc(entId, "../entity/getModel?id=" + entId);
               		</script>
               		<xf:rebuild model="rmodel"/>
               	</xf:action>
   			</xf:select1></td>
   			
            <td>where</td>
            <td style="padding-left:10px"><xf:select1 ref="@field">
   				<xf:label/>
                <xf:item><xf:label>-Select&#160;Field-</xf:label><xf:value/></xf:item>
                <xf:itemset nodeset="instance(../@type)/data/attr[@browse != '']">
                	<xf:label ref="@label"/>
                	<xf:value ref="@name"/>
                </xf:itemset>
                </xf:select1></td>
            <td>
                <xf:select1 ref="@op" style="width:6em">
                     <xf:label/>
                     <xf:item><xf:label>greater than</xf:label><xf:value>&gt;</xf:value></xf:item>
                     <xf:item><xf:label>less than</xf:label><xf:value>&lt;</xf:value></xf:item>
                     <xf:item><xf:label>equal to</xf:label><xf:value>=</xf:value></xf:item>
                     <xf:item><xf:label>not equal to</xf:label><xf:value>!=</xf:value></xf:item>
                 </xf:select1>
            </td>
            <td>
                <xf:input ref="@arg" style="width:8em;font-size:11px" stopdrag="true">
                     <xf:label/>
                 </xf:input>
            </td>
            <td style="padding:2px 10px 2px 10px"><u style="cursor:pointer" title="Add find condition" stopdrag="true"><xf:action ev:event="click">
            	<xf:duplicate ref="." origin="instance('pal')/and"/>
            </xf:action>and...</u></td>
            <td>
            	<div class="traceMsg"></div>
            </td>
         </tr>
         <xf:repeat nodeset="and">
         <tr>
         	<td colspan="2"></td>
         	<td>and</td>
         	<td style="padding-left:10px"><xf:select1 ref="@field">
   				<xf:label/>
                <xf:item><xf:label>-Select&#160;Field-</xf:label><xf:value/></xf:item>
                <xf:itemset nodeset="instance(../../@type)/data/attr[@browse != '']">
                	<xf:label ref="@label"/>
                	<xf:value ref="@name"/>
                </xf:itemset>
                </xf:select1></td>
            <td>
                <xf:select1 ref="@op" style="width:6em">
                     <xf:label/>
                     <xf:item><xf:label>greater than</xf:label><xf:value>&gt;</xf:value></xf:item>
                     <xf:item><xf:label>less than</xf:label><xf:value>&lt;</xf:value></xf:item>
                     <xf:item><xf:label>equal to</xf:label><xf:value>=</xf:value></xf:item>
                     <xf:item><xf:label>not equal to</xf:label><xf:value>!=</xf:value></xf:item>
                 </xf:select1>
            </td>
            <td>
                <xf:input ref="@arg" style="width:8em;font-size:11px" stopdrag="true">
                     <xf:label/>
                 </xf:input>
            </td>
            <td style="padding:2px 10px 2px 10px">
            <span title="Remove 'and'" style="cursor:pointer" stopdrag="true">X<xf:action ev:event="click"> <xf:destroy ref="."/> </xf:action></span></td>
            <td></td>
          </tr>
         </xf:repeat>
         </tbody>
     </table>
    </div>
</ix:template>


<ix:template name="_num_field">
	<div class="cont">
	<div class="delete"><span title="Remove" stopdrag="true">X<xf:action ev:event="click"> <xf:destroy ref="."/> </xf:action></span></div>
     <table>
         <tr>
            <td>If:</td>
            <td class="field"><xf:output ref="@field"/></td>
            <td>
                <xf:select1 ref="@op" style="width:6em">
                     <xf:label/>
                     <xf:item><xf:label>greater than</xf:label><xf:value>&gt;</xf:value></xf:item>
                     <xf:item><xf:label>less than</xf:label><xf:value>&lt;</xf:value></xf:item>
                     <xf:item><xf:label>equal to</xf:label><xf:value>=</xf:value></xf:item>
                     <xf:item><xf:label>not equal to</xf:label><xf:value>!=</xf:value></xf:item>
                 </xf:select1>
            </td>
            <td>
                <xf:input ref="@arg" style="width:8em;font-size:11px"  stopdrag="true">
                     <xf:label/>
                 </xf:input>
            </td>
            <td>
            	<div class="traceMsg"></div>
            </td>
         </tr>
     </table>
    </div>
</ix:template>

<ix:template name="_arr_field">
	<div class="cont">
	<div class="delete"><span title="Remove" stopdrag="true">X<xf:action ev:event="click"> <xf:destroy ref="."/> </xf:action></span></div>
     <table>
         <tr>
            <td>If:</td>
            <td>
                <xf:select1 ref="@aggregate" style="width:10em">
                     <xf:label/>
                     <xf:item><xf:label>the first</xf:label><xf:value></xf:value></xf:item>
                     <xf:item><xf:label>count of</xf:label><xf:value>count</xf:value></xf:item>
                     <xf:item><xf:label>sum of</xf:label><xf:value>sum</xf:value></xf:item>
                     <xf:item><xf:label>average of</xf:label><xf:value>avg</xf:value></xf:item>
                     <xf:item><xf:label>minimum of</xf:label><xf:value>min</xf:value></xf:item>
                     <xf:item><xf:label>maximum of</xf:label><xf:value>max</xf:value></xf:item>
                     <xf:item><xf:label>standard deviation of</xf:label><xf:value>stddev</xf:value></xf:item>
                 </xf:select1>
            </td>
            <td class="field"><xf:output ref="@field"/></td>
            <td>
                <xf:select1 ref="@op" style="width:6em">
                     <xf:label/>
                     <xf:item><xf:label>greater than</xf:label><xf:value>&gt;</xf:value></xf:item>
                     <xf:item><xf:label>less than</xf:label><xf:value>&lt;</xf:value></xf:item>
                     <xf:item><xf:label>equal to</xf:label><xf:value>=</xf:value></xf:item>
                     <xf:item><xf:label>not equal to</xf:label><xf:value>!=</xf:value></xf:item>
                 </xf:select1>
            </td>
            <td>
                <xf:input ref="@arg" style="width:8em;font-size:11px" stopdrag="true">
                     <xf:label/>
                 </xf:input>
            </td>
            <td>
            	<div class="traceMsg"></div>
            </td>
         </tr>
     </table>
    </div>
</ix:template>

<ix:template name="when-xsd:string" class="field"><ix:include template="_num_field" nodiv="1"/></ix:template>
<ix:template name="when-xsd:float" class="field"><ix:include template="_num_field" nodiv="1"/></ix:template>
<ix:template name="when-xsd:float:array" class="field"><ix:include template="_arr_field" nodiv="1"/></ix:template>
<ix:template name="when-ix:currencyUSD" class="field"><ix:include template="_num_field" nodiv="1"/></ix:template>
<ix:template name="when-ix:currencyUSD:array" class="field"><ix:include template="_arr_field" nodiv="1"/></ix:template>
<ix:template name="when-ix:percent" class="field"><ix:include template="_num_field" nodiv="1"/></ix:template>
<ix:template name="when-ix:percent:array" class="field"><ix:include template="_arr_field" nodiv="1"/></ix:template>
<ix:template name="when-xsd:NMTOKEN" class="field"><ix:include template="_num_field" nodiv="1"/></ix:template>
<ix:template name="when-xsd:NMTOKEN:array" class="field"><ix:include template="_arr_field" nodiv="1"/></ix:template>
<ix:template name="when-xsd:NMTOKENS" class="field"><ix:include template="_num_field" nodiv="1"/></ix:template>
<ix:template name="when-xsd:boolean" class="field"><ix:include template="_num_field" nodiv="1"/></ix:template>
<ix:template name="when-xsd:date" class="field"><ix:include template="_num_field" nodiv="1"/></ix:template>
<ix:template name="when-xsd:dateTime" class="field"><ix:include template="_num_field" nodiv="1"/></ix:template>
<ix:template name="when-ix:email" class="field"><ix:include template="_num_field" nodiv="1"/></ix:template>
<ix:template name="when-ix:file" class="field"><ix:include template="_num_field" nodiv="1"/></ix:template>
<ix:template name="when-ix:http" class="field"><ix:include template="_num_field" nodiv="1"/></ix:template>
<ix:template name="when-ix:user" class="field"><ix:include template="_num_field" nodiv="1"/></ix:template>
<ix:template name="when-ix:userGroup" class="field"><ix:include template="_num_field" nodiv="1"/></ix:template>

</form>