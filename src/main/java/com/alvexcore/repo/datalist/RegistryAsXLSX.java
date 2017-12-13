package com.alvexcore.repo.datalist;

import com.alvexcore.repo.foldersize.FolderSizeService;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
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

    private DictionaryService dictionaryService;

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
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
//                System.out.println(dictionaryService.getProperty(QName.createQName(property)));

//                System.out.println(QName.resolveToQName(namespaceService, property));
//                dictionaryService.getType(nodeService.getType(itemsNodes.get(0)));
//                System.out.println("-" + dictionaryService.getProperty(QName.resolveToQName(namespaceService, property)));
//                System.out.println(null);
//                if (dictionaryService.getProperty(QName.resolveToQName(namespaceService, property))==null){
                if (QName.resolveToQName(namespaceService, property)==null){
                    webScriptResponse.setStatus(Status.STATUS_BAD_REQUEST);
//                  webScriptResponse.getWriter().write();
                    throw new WebScriptException(Status.STATUS_BAD_REQUEST, "ContentModel doesn't contain property: " + property);
                }
            }

            if (dataListNode==null) {
                List<NodeRef> itemsNodes = new ArrayList<>();
                for (int k=0; k<siteNodes.toArray().length; itemsNodes.add(new NodeRef(siteNodes.get(k++))));

                wb = createWorkbook(XLS_SHEET_NAME, includeList, excludeList, itemsNodes);
//                wb = createXlsxRegisters(XLS_SHEET_NAME, siteNodes, includeList, excludeList);
            }
            else {
                List<ChildAssociationRef> itemsNodesTest = nodeService.getChildAssocs(new NodeRef(dataListNode), ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL);
                List<NodeRef> itemsNodes = new ArrayList<>();
                for (int k=0; k<itemsNodesTest.toArray().length; itemsNodes.add(itemsNodesTest.get(k++).getChildRef()));

                wb = createWorkbook(XLS_SHEET_NAME, includeList, excludeList, itemsNodes);

//                System.out.println(nodeService.getChildAssocs(new NodeRef(dataListNode), ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL).size());
//                wb = createXlsxRegisters(XLS_SHEET_NAME, dataListNode, includeList, excludeList);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);

            webScriptResponse.setContentEncoding("UTF-8");
            webScriptResponse.setContentType(MimetypeMap.MIMETYPE_EXCEL);
            webScriptResponse.getOutputStream().write(baos.toByteArray());
//            MimetypeMap.
        } catch (WebScriptException e) {
            webScriptResponse.setStatus(Status.STATUS_BAD_REQUEST);
            throw new WebScriptException(Status.STATUS_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
//            webScriptResponse.setStatus(400);
//            System.out.println("Didn't work " + e);
//            System.out.println(Status.STATUS_BAD_REQUEST);
//            webScriptResponse.setStatus(Status.STATUS_BAD_REQUEST);
//            webScriptResponse.getWriter().write();
//            throw new WebScriptException("Something went horribly wrong.");

        }
    }


    protected Workbook createWorkbook (String XLS_SHEET_NAME, List<String> includeList, List<String> excludeList, List<NodeRef> itemsNodes ){

        Workbook wb = new XSSFWorkbook();

        CreationHelper createHelper = wb.getCreationHelper();
        Sheet sheet = wb.createSheet(XLS_SHEET_NAME);

        int rowNum = 0;

        if (includeList.toArray().length>0) {
            Map<QName, Serializable> map = new HashMap<QName, Serializable>();
            List<QName> headers = new ArrayList<>();
            List<String> values = new ArrayList<>();

            for (String str: includeList){
//                dictionaryService.getProperty(QName.resolveToQName(namespaceService, str))
//                System.out.println(QName.resolveToQName(namespaceService, str));
//                if (nodeService.getProperty(itemsNodes.get(0), QName.resolveToQName(namespaceService, str))!=null) {
//                if (QName.resolveToQName(namespaceService, str)!=null) {
                 if (dictionaryService.getProperty(QName.resolveToQName(namespaceService, str))!=null) {
//                    headers.add(dictionaryService.getProperty(QName.resolveToQName(namespaceService, str)).getName());
                     headers.add(QName.resolveToQName(namespaceService, str));
                 }
            }
//            System.out.println(dictionaryService.getProperty((QName) map.keySet().toArray()[0]));

            Row row = fillRowHeader(rowNum, headers, createHelper, sheet);
            rowNum++;

            for (NodeRef item : itemsNodes) {
                map.clear();
                values.clear();
//                for (String str: includeList){
                for (QName str: headers){
//                    System.out.println(nodeService.getProperty(item, str));

//                    if (nodeService.getProperty(item, QName.resolveToQName(namespaceService, str))!=null)
//                    map.put(str, nodeService.getProperty(item, str));
                    values.add(Objects.toString(nodeService.getProperty(item, str), ""));
                }

                row = fillRowData(rowNum, values, createHelper, sheet);
                rowNum++;
            }
        }
        else {
            QName type1 = nodeService.getType(itemsNodes.get(0));
//            dictionaryService.getType(type1).getProperties().
//            nodeService.getProperty();
//            dictionaryService.getType(nodeService.getType(itemsNodes.get(0)));
            Map<QName, PropertyDefinition> map = dictionaryService.getType(type1).getProperties();
//            Map<QName, Serializable> map = nodeService.getProperties(itemsNodes.get(0));
            List<QName> headers = new ArrayList<>();
            List<String> values = new ArrayList<>();

            headers.addAll(map.keySet());

            //            headers = (List<QName>) map.keySet();
//            System.out.println(headers);

            for (String str : excludeList) {
//                System.out.println("+" + dictionaryService.getProperties(ContentModel.PROP_NAME));
//                headers.add(QName.resolveToQName(namespaceService, str));
                headers.remove(QName.resolveToQName(namespaceService, str));
            }

            Row row = fillRowHeader(rowNum, headers, createHelper, sheet);
            rowNum++;

            for (NodeRef item : itemsNodes) {
                values.clear();
//                map = nodeService.getProperties(item);
                for (QName str : headers) {
//                    map.remove(QName.resolveToQName(namespaceService, str));
//                    System.out.println(nodeService.getProperty(item, str));
                    values.add(Objects.toString(nodeService.getProperty(item, str), ""));
                }

                row = fillRowData(rowNum, values, createHelper, sheet);
                rowNum++;
            }
        }

        return wb;
    }


    private Row fillRowData(int rowNum, List<String> values, CreationHelper createHelper, Sheet sheet)  {
        int cellNum = 0;
        Row row = sheet.createRow((short) rowNum);
        for (String i : values) {
            row.createCell(cellNum).setCellValue(createHelper.createRichTextString(i.toString()));
            cellNum++;
        }
        return row;
    }

    private Row fillRowHeader(int rowNum,  List<QName> headers, CreationHelper createHelper, Sheet sheet)  {
        int cellNum = 0;
        Row row = sheet.createRow((short) rowNum);
        for (QName i : headers) {
            row.createCell(cellNum).setCellValue(createHelper.createRichTextString(i.toPrefixString(namespaceService)));
            cellNum++;
        }
        return row;
    }
}