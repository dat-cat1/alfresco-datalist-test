<html>
<head>
    <title>Folder ${folder.displayPath}/${folder.name}</title>
</head>
<body>
<p>Alfresco ${server.edition} Edition v${server.version} : dir</p>
<p>Contents of folder ${folder.displayPath}/${folder.name}</p>
<table>
    <#list Folder as child>
        <tr>
            <td><#if child.element.isContainer>d</#if></td>
            <td>${child.element.name}</td>
            <td>${child.size} B</td>
            <#if verbose>
                <td>${child.element.properties.modifier}</td>
                <td><#if child.element.isDocument>
            ${child.element.properties.content.size}</#if></td>
                <td>${child.element.properties.modified?date}</td>
                <td>${child.element.properties.modified?date}</td>
            </#if>
        </tr>
    </#list>
</table>
</body>
</html>