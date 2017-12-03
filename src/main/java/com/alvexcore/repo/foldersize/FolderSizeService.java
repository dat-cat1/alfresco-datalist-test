package com.alvexcore.repo.foldersize;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.springframework.extensions.surf.util.Content;
import org.springframework.extensions.webscripts.WebScriptRequest;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//import java.util.stream.Collectors;

/**
 * Created by dtsvetkov on 05.07.17.
 */
public class FolderSizeService {


    private NodeService nodeService;

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    private Repository repository;

    public void setRepository(Repository repository)
    {
        this.repository = repository;
    }


    float foldersize(NodeRef directory, Float length) {

        length = Float.valueOf(0);
        for (ChildAssociationRef child: nodeService.getChildAssocs(directory)) {

            ContentData contentData = (ContentData) nodeService.getProperty(child.getChildRef(), ContentModel.PROP_CONTENT);

            try {
                length += Float.valueOf(contentData.getSize());
            } catch (Exception e) {
                length +=  foldersize(child.getChildRef(), length);
            }

        }
        return length;
    }

    public List<Map>  getFolderChildren (WebScriptRequest req) {

        NodeRef folder;
        Float size = Float.valueOf(0);

        // extract folder listing arguments from URI
        Map<String, String> templateArgs =
                req.getServiceMatch().getTemplateVars();
        String folderPath = templateArgs.get("folderpath");

        if ((folderPath.equals("Company Home")) || (folderPath.equals("Company Home/")) ){
            folder = repository.getCompanyHome();
        }
        else {
            String nodePath = "workspace/SpacesStore/" + folderPath;
            folder = repository.findNodeRef("path", nodePath.split("/"));
        }

        List<Map> total_list = new ArrayList<>();
        List<ChildAssociationRef> ChildAssoc = nodeService.getChildAssocs(folder);

        for (ChildAssociationRef child: ChildAssoc) {
            size = Float.valueOf(0);
            Map<String, Object> Folder = new HashMap<>();
            NodeRef cnode = child.getChildRef();
            Map<QName, Serializable> props = nodeService.getProperties(cnode);

            try {
                ContentData contentData = (ContentData) props.get(ContentModel.PROP_CONTENT);
                Float fileSize = Float.valueOf(contentData.getSize());
                Folder.put("size", fileSize);
            }
            catch (Exception e) {
                Folder.put("size", foldersize(cnode, size));
            }

            Folder.put("element", cnode);
            Folder.put("propertyName", props.get(ContentModel.PROP_NAME));

            total_list.add(Folder);
        }

        return  total_list;
    }


    public NodeRef getFolderNodeRef(WebScriptRequest req) {

        NodeRef folder;

        Map<String, String> templateArgs =
                req.getServiceMatch().getTemplateVars();
        String folderPath = templateArgs.get("folderpath");

        if ((folderPath.equals("Company Home")) || (folderPath.equals("Company Home/")) ){

            folder = repository.getCompanyHome();

        }
        else {
            String nodePath = "workspace/SpacesStore/" + folderPath;
            folder = repository.findNodeRef("path", nodePath.split("/"));
        }

        return folder;
    }

    public String getFolderName(WebScriptRequest req) {

        NodeRef folder = getFolderNodeRef(req);
        Map<QName, Serializable> props = nodeService.getProperties(folder);

        return (String) props.get(ContentModel.PROP_NAME);
    }

    public String[] getresponse(WebScriptRequest req) {

        Content folder = req.getContent();
        InputStream stream = folder.getInputStream();
        String[] params = req.getParameterNames();

        return params;
    }

    public Boolean getVerbose (WebScriptRequest req) {
        String verboseArg = req.getParameter("verbose");
        Boolean verbose = Boolean.parseBoolean(verboseArg);
        return verbose;
    }
}
