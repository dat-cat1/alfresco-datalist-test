{
"server" : "Alfresco ${server.edition} Edition v${server.version}",
<#--"folder" :-->
{
"path" : "${folder.displayPath}",
"name" : "${folder.name}"
},
"children" : [
<#list Folder as child>
{
"isfolder" : <#if child.element.isContainer>true<#else>false</#if>,
    <#if verbose>
    "modifier" : "${child.element.properties.modifier}",
    "size" : <#if child.element.isDocument>
    ${child.element.properties.content.size?c}<#else>null</#if>,
    "modified" : "${child.element.properties.modified?date}",
    </#if>
"name" : "${child.element.name}",
"size" : "${child.size}"
}<#if child_has_next>,</#if>
</#list>
]
}