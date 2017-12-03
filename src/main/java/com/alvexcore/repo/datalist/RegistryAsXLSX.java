package com.alvexcore.repo.datalist;

import com.alvexcore.repo.foldersize.FolderSizeService;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.*;
import org.alfresco.service.namespace.NamespaceService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.*;
import java.io.*;
import java.util.*;

public class RegistryAsXLSX extends AbstractWebScript {


    private String DATALIST_CONTAINER_ID;
    private FolderSizeService folderSizeService;

    public void setFolderSizeService(FolderSizeService folderSizeService) { this.folderSizeService = folderSizeService; }

    private NamespaceService namespaceService;

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    private NodeService nodeService;

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    private SiteService siteService;

    public void setSiteService(SiteService siteService) { this.siteService = siteService; }


    @Override
    public void execute(WebScriptRequest webScriptRequest, WebScriptResponse webScriptResponse) throws IOException {
        Map<String, Object> model = new HashMap<>();

        final String FOLDER_NAME = "datalists-exports";
        final String XLS_SHEET_NAME = "DataList";

        Reader reader = webScriptRequest.getContent().getReader();
        BufferedReader bufferReader = new BufferedReader(reader);

        String dataListNode = null;
        JSONObject jsonObj = new JSONObject();
        JSONArray nodesList = new JSONArray();
        JSONArray include = null;
        JSONArray exclude = null;
        List<String> siteNodes = new ArrayList<String>();
        List<String> includeList = new ArrayList<String>();
        List<String> excludeList = new ArrayList<String>();

        try {
//            System.out.println(bufferReader.readLine());
            jsonObj = new JSONObject(bufferReader.readLine());
            if (jsonObj.get("NodeRefs").getClass()==JSONArray.class) {
                nodesList = jsonObj.getJSONArray("NodeRefs");
            }
            else {
                dataListNode = jsonObj.getString("NodeRefs");
            }
            if (jsonObj.has("include")) {
                include = jsonObj.getJSONArray("include");
            }
            else if ((jsonObj.has("exclude"))) {
                exclude = jsonObj.getJSONArray("exclude");
            }

        if (dataListNode==null) {
            for(int i = 0; i < nodesList.length(); siteNodes.add((String) nodesList.get(i++)));
        }

        if (include!=null) {
//            System.out.println(include.get(0));
            for (int i = 0; i < include.length(); includeList.add((String) include.get(i++)));
        }
        else if (exclude!=null) {
            for (int i = 0; i < exclude.length(); excludeList.add((String) exclude.get(i++)));
        }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {

            Workbook wb = null;
            for (String property: includeList){
                if (!namespaceService.getPrefixes().contains(property.split(":")[0])){
                    webScriptResponse.setStatus(Status.STATUS_BAD_REQUEST);
//            webScriptResponse.getWriter().write();
                    throw new WebScriptException(Status.STATUS_BAD_REQUEST, "ContentModel doesn't contain property: " + property);

                }
//                System.out.println(property.split(":")[0]);
//                System.out.println(namespaceService.getPrefixes());
            }

            if (dataListNode==null) {
                wb = createXlsxRegisters(XLS_SHEET_NAME, siteNodes, includeList, excludeList);
            }
            else {
//                System.out.println(nodeService.getChildAssocs(new NodeRef(dataListNode), ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL).size());
                wb = createXlsxRegisters(XLS_SHEET_NAME, dataListNode, includeList, excludeList);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);

            webScriptResponse.setContentEncoding("UTF-8");
            webScriptResponse.setContentType(MimetypeMap.MIMETYPE_EXCEL);
            webScriptResponse.getOutputStream().write(baos.toByteArray());

        } catch (NullPointerException e) {

//            webScriptResponse.setStatus(400);
//            System.out.println("Didn't work " + e);
//            System.out.println(Status.STATUS_BAD_REQUEST);
//            webScriptResponse.setStatus(Status.STATUS_BAD_REQUEST);
//            webScriptResponse.getWriter().write();
//            throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Something went horribly wrong");

        }
    }


    protected Workbook createXlsxRegisters(String XLS_SHEET_NAME, String dataListNode, List<String> includeList, List<String> excludeList) {

        try {
            Workbook wb = new XSSFWorkbook();
            CreationHelper createHelper = wb.getCreationHelper();
            Sheet sheet = wb.createSheet(XLS_SHEET_NAME);

            int rowNum = 0;
            int cellNum;

            List<ChildAssociationRef> itemsNodes = nodeService.getChildAssocs(new NodeRef(dataListNode), ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL);

            if (includeList.toArray().length>0) {
//                Map<QName, Serializable> variable = nodeService.getProperties(itemsNodes.get(0).getChildRef());
                Map<QName, Serializable> map = new HashMap<QName, Serializable>();
                for (String str: includeList){
                    if (nodeService.getProperty(itemsNodes.get(0).getChildRef(), QName.resolveToQName(namespaceService, str))!=null)
                    map.put(QName.resolveToQName(namespaceService, str) , nodeService.getProperty(itemsNodes.get(0).getChildRef(), QName.createQName(str, namespaceService)));
                }
                cellNum = 0;
                Row row = sheet.createRow((short) rowNum);
                for (QName i : map.keySet()) {
                        row.createCell(cellNum).setCellValue(createHelper.createRichTextString(i.toPrefixString(namespaceService)));
                        cellNum++;
                }
                rowNum++;

                for (ChildAssociationRef item : itemsNodes) {

                    map.clear();
                    for (String str: includeList){
                        if (nodeService.getProperty(item.getChildRef(), QName.resolveToQName(namespaceService, str))!=null)
                            map.put(QName.resolveToQName(namespaceService, str) , nodeService.getProperty(item.getChildRef(), QName.resolveToQName(namespaceService, str)));
                    }

                    cellNum = 0;
                    row = sheet.createRow((short) rowNum);
                    for (QName i : map.keySet()) {
                        row.createCell(cellNum).setCellValue(createHelper.createRichTextString(map.get(i).toString()));
                        cellNum++;
                    }
                    rowNum++;

                }
            }
            else {
                Map<QName, Serializable> map = nodeService.getProperties(itemsNodes.get(0).getChildRef());
                for (String str: excludeList){
//                    if (nodeService.getProperty(itemsNodes.get(0).getChildRef(), QName.resolveToQName(namespaceService, str))!=null)
                    map.remove(QName.resolveToQName(namespaceService, str));
                }
                cellNum = 0;
                Row row = sheet.createRow((short) rowNum);
                for (QName i : map.keySet()) {
                    row.createCell(cellNum).setCellValue(createHelper.createRichTextString(i.toPrefixString(namespaceService)));
                    cellNum++;
                }
                rowNum++;

                for (ChildAssociationRef item : itemsNodes) {

//                    map.clear();
                    map = nodeService.getProperties(item.getChildRef());

                    for (String str: excludeList){
//                        if (nodeService.getProperty(item.getChildRef(), QName.resolveToQName(namespaceService, str))!=null)
                        map.remove(QName.resolveToQName(namespaceService, str));
                    }

                    cellNum = 0;
                    row = sheet.createRow((short) rowNum);
                    for (QName i : map.keySet()) {
                        row.createCell(cellNum).setCellValue(createHelper.createRichTextString(map.get(i).toString()));
                        cellNum++;
                    }
                    rowNum++;

                }
            }
            return wb;

        } catch (Exception e) {
            //
        }
        return null;
    }


    protected Workbook createXlsxRegisters(String XLS_SHEET_NAME, List<String> siteNodes, List<String> includeList, List<String> excludeList) {

        try {

            Workbook wb = new XSSFWorkbook();
            CreationHelper createHelper = wb.getCreationHelper();
            Sheet sheet = wb.createSheet(XLS_SHEET_NAME);

            int rowNum = 0;
            int cellNum = 0;

            Row row = sheet.createRow((short) rowNum);
//            System.out.println(namespaceService.getPrefixes());
            if (includeList.toArray().length>0) {
                Map<QName, Serializable> map = new HashMap<QName, Serializable>();
                for (String str: includeList){
                    if (nodeService.getProperty(new NodeRef(siteNodes.get(0)), QName.resolveToQName(namespaceService, str))!=null)
                    map.put(QName.resolveToQName(namespaceService, str) , nodeService.getProperty(new NodeRef(siteNodes.get(0)), QName.resolveToQName(namespaceService, str)));
                }

                for (QName i : map.keySet()) {
                        row.createCell(cellNum).setCellValue(createHelper.createRichTextString(i.toPrefixString(namespaceService)));
                        cellNum++;
                }
                rowNum++;

                for (String j : siteNodes) {

                    Map<QName, Serializable> itemsNodes = new HashMap<QName, Serializable>();
                    for (String str: includeList){
                        if (nodeService.getProperty(new NodeRef(j), QName.resolveToQName(namespaceService, str))!=null) {
                            itemsNodes.put(QName.resolveToQName(namespaceService, str), nodeService.getProperty(new NodeRef(j), QName.resolveToQName(namespaceService, str)));
                        }
                    }

                    cellNum = 0;
                    row = sheet.createRow((short) rowNum);
                    for (QName i : itemsNodes.keySet()) {
//                        System.out.println(itemsNodes);
                            row.createCell(cellNum).setCellValue(createHelper.createRichTextString(itemsNodes.get(i).toString()));
                            cellNum++;
                    }
                    rowNum++;

                }
            }
            else {
                Map<QName, Serializable> map = nodeService.getProperties(new NodeRef(siteNodes.get(0)));
                for (String i: excludeList) {
                    map.remove(QName.resolveToQName(namespaceService, i));
                }

                for (QName i : map.keySet()) {

                    row.createCell(cellNum).setCellValue(createHelper.createRichTextString(i.toPrefixString(namespaceService)));
                    cellNum++;
                }
                rowNum++;

                for (String j: siteNodes) {
//                    System.out.println(j);
                    Map<QName, Serializable> itemsNodes = nodeService.getProperties(new NodeRef(j));
                    for (String i: excludeList) {
                        itemsNodes.remove(QName.resolveToQName(namespaceService, i));
                    }
                    cellNum = 0;
                    row = sheet.createRow((short) rowNum);
                    for (QName i : itemsNodes.keySet()) {
                        row.createCell(cellNum).setCellValue(createHelper.createRichTextString(itemsNodes.get(i).toString()));
                        cellNum++;
                    }
                    rowNum++;

                }
            }

            return wb;

        } catch (Exception e) {
            //
        }
        return null;
    }
}