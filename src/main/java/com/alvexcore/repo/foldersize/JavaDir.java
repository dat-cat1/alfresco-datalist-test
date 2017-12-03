package com.alvexcore.repo.foldersize;

import java.util.*;


import org.alfresco.repo.model.Repository;
//import org.alfresco.service.NodeRef;
import org.alfresco.service.cmr.repository.*;
import org.springframework.extensions.webscripts.*;


public class JavaDir extends DeclarativeWebScript
{


    private FolderSizeService folderSizeService;

    public void setFolderSizeService(FolderSizeService folderSizeService) {
        this.folderSizeService = folderSizeService;
    }
//    public void Folder(FileFolderService fileFolderService)
//    {
//        this.fileFolderService = fileFolderService;
//    }


    private NodeService nodeService;
//    private FolderSizeService FolderSizeService;

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    private Repository repository;

    public void setRepository(Repository repository)
    {
        this.repository = repository;
    }


    protected Map<String, Object> executeImpl(WebScriptRequest req,
                                              Status status, Cache cache)
    {

        Boolean verbose = folderSizeService.getVerbose(req);

        List<Map> total_list = folderSizeService.getFolderChildren(req);

        NodeRef folder = folderSizeService.getFolderNodeRef(req);

        // construct model for response template to render
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("verbose", verbose);
        model.put("folder", folder);
        model.put("Folder", total_list);
        return model;
    }



}