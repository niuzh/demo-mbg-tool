package com.funi.demo;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.mybatis.generator.api.*;
import org.mybatis.generator.config.*;
import org.mybatis.generator.config.xml.ConfigurationParser;
import org.mybatis.generator.exception.XMLParserException;
import org.mybatis.generator.internal.NullProgressCallback;
import org.mybatis.generator.internal.util.JavaBeansUtil;
import org.xml.sax.InputSource;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

/**
 * @author zhihuan.niu on 7/31/17.
 */
public class CodeGeneratorForm extends JFrame {
    private JPanel panelHeader;
    private JComboBox comboBoxResource;
    private JComboBox comboBoxTable;
    private JComboBox comboBoxCode;
    private JTextField textFieldFileName;
    private JButton btnClipboard;
    private JButton btnFile;
    private JTextArea textArea;
    private Map<String,IntrospectedTable> map=new HashMap<String,IntrospectedTable>();
    List<GeneratedJavaFile> generatedJavaFiles=new ArrayList<GeneratedJavaFile>();
    List<GeneratedXmlFile> generatedXmlFiles=new ArrayList<GeneratedXmlFile>();
    //todo 根据app项目名修改 针对ExtJS
    private static final String packagePrefix="app.platform.credit";
    private String tablePreName="";
    public CodeGeneratorForm() throws IOException, XMLParserException {
        this.setLayout(new BorderLayout());
        this.panelHeader =new JPanel();
        comboBoxResource =new JComboBox();
        comboBoxTable =new JComboBox();
        comboBoxCode =new JComboBox();
        btnClipboard =new JButton("复制代码");
        btnFile =new JButton("生成代码文件");
        textFieldFileName=new JTextField();
        textFieldFileName.setColumns(10);
        panelHeader.setLayout(new FlowLayout());
        panelHeader.add(new JLabel("资源文件"));
        panelHeader.add(comboBoxResource);
        panelHeader.add(new JLabel("数据表"));
        panelHeader.add(comboBoxTable);
        panelHeader.add(new JLabel("代码"));
        panelHeader.add(comboBoxCode);
        panelHeader.add(textFieldFileName);
        panelHeader.add(btnClipboard);
        panelHeader.add(btnFile);
        comboBoxCode.addItem("清空");
        comboBoxCode.addItem("Dto");
        comboBoxCode.addItem("Mapper");
        comboBoxCode.addItem("Mapper.XML");
        comboBoxCode.addItem("ExtJSModel");
        this.getContentPane().add("North", this.panelHeader);
        textArea=new JTextArea();
        JScrollPane jScrollPane=new JScrollPane(textArea);
        jScrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        getContentPane().add(jScrollPane);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);//让窗体居中显示
        File[] files = new File(this.getClass().getClassLoader().getResource(".").getPath()).listFiles();
        for (File file:files){
            if(file.getName().endsWith(".cfg.xml")){
                comboBoxResource.addItem(file.getName());
            }
        }
        comboBoxResource.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String resourceName=comboBoxResource.getSelectedItem().toString();
                try {
                    initCfgXML(resourceName);
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (XMLParserException e1) {
                    e1.printStackTrace();
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                } catch (IllegalAccessException e1) {
                    e1.printStackTrace();
                } catch (NoSuchFieldException e1) {
                    e1.printStackTrace();
                }
            }
        });
        comboBoxCode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedItem=comboBoxCode.getSelectedItem().toString();
                String selectedTable=comboBoxTable.getSelectedItem().toString();
                IntrospectedTable introspectedTable=map.get(selectedTable);
                String objectName = introspectedTable.getTableConfiguration().getDomainObjectName();
                String text="/**\n" +
                        " * @author 工具自动生成\n" +
                        " */\n";
                if("清空".equals(selectedItem)){
                    textArea.setText("");
                    objectName+="";
                }
                if("Dto".equals(selectedItem)){
                    text += getDtoString(introspectedTable);
                    text=text.replace(tablePreName+objectName,objectName);
                    objectName+=".java";
                }
                if("Mapper".equals(selectedItem)){
                    text += getMapperString(introspectedTable);
                    text=text.replace(tablePreName+objectName,objectName);
                    objectName+="Mapper.java";
                }
                if("Mapper.XML".equals(selectedItem)){
                    text = getMapperXMLString(introspectedTable);
                    text=text.replace(tablePreName+objectName,objectName);
                    objectName+="Mapper.xml";
                }
                if("ExtJSModel".equals(selectedItem)){
                    text += getModelString(introspectedTable);
                    text=text.replace(tablePreName+objectName,objectName);
                    objectName+="Model.js";
                }
                System.out.println(tablePreName+objectName);
                text=text.replace(tablePreName+objectName,objectName);
                textArea.setText(text);
                textFieldFileName.setText(objectName);
                if("Mapper.XML".equals(selectedItem)){
                    try {
                        dealXml();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
        btnClipboard.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                Transferable trandata = new StringSelection(textArea.getText());
                clipboard.setContents(trandata, null);
            }
        });
        btnFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IntrospectedTable introspectedTable=map.get(comboBoxTable.getSelectedItem().toString());
                String selectedItem=comboBoxCode.getSelectedItem().toString();
                String targetProjectBase=introspectedTable.getContext().getProperty("targetProject");
                String targetProject="";
                String targetPackage="";
                if("Dto".equals(selectedItem)){
                    JavaModelGeneratorConfiguration javaModelGeneratorConfiguration=introspectedTable.getContext().getJavaModelGeneratorConfiguration();
                    targetProject=javaModelGeneratorConfiguration.getTargetProject();
                    targetPackage=javaModelGeneratorConfiguration.getTargetPackage().replace(".",File.separator);
                }
                if("Mapper".equals(selectedItem)){
                    JavaClientGeneratorConfiguration javaClientGeneratorConfiguration=introspectedTable.getContext().getJavaClientGeneratorConfiguration();
                    targetProject=javaClientGeneratorConfiguration.getTargetProject();
                    targetPackage=javaClientGeneratorConfiguration.getTargetPackage().replace(".",File.separator);
                }
                if("Mapper.XML".equals(selectedItem)){
                    SqlMapGeneratorConfiguration sqlMapGeneratorConfiguration=introspectedTable.getContext().getSqlMapGeneratorConfiguration();
                    targetProject=sqlMapGeneratorConfiguration.getTargetProject();
                    targetPackage=sqlMapGeneratorConfiguration.getTargetPackage().replace(".",File.separator);
                }
                if("ExtJSModel".equals(selectedItem)){
                    targetProject="webapp"+File.separator+packagePrefix.replace(".",File.separator);
                    targetPackage="model";
                }
                String filePath=targetProjectBase+File.separator+targetProject+File.separator+targetPackage+File.separator+textFieldFileName.getText();
                System.out.println(filePath);
                String text=textArea.getText();
                contentToTxt(filePath,text);
            }
        });
    }
    private void contentToTxt(String filePath, String content) {
        String strTemp = new String(); //原有txt内容
        String strOld = new String();//内容更新
        try {
            File f = new File(filePath);
            if (f.exists()) {
                System.out.print("文件存在");
            } else {
                System.out.print("文件不存在");
                f.createNewFile();// 不存在则创建
            }
            BufferedReader input = new BufferedReader(new FileReader(f));
            while ((strTemp = input.readLine()) != null) {
                strOld += strTemp + "\n";
            }
            //System.out.println(strOld);
            input.close();
            //strOld += content;

            BufferedWriter output = new BufferedWriter(new FileWriter(f));
            output.write(content);
            output.close();
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    private void dealXml() throws IOException {
        StringReader sr = new StringReader(textArea.getText());
        InputSource is = new InputSource(sr);
        SAXReader reader = new SAXReader();
        //读取文件 转换成Document
        Document document = null;
        try {
            document = reader.read(is);
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        //获取根节点元素对象
        Element root = document.getRootElement();
        //同时迭代当前节点下面的所有子节点
        //使用递归
        List<Element> elementList = root.elements();
        for (Element element:elementList){
            System.out.println(element.asXML());
            Attribute attribute = element.attribute("id");
            if("insert".equals(attribute.getValue())) {
                element.setText(element.getText().replace("#{sysCreateTime,jdbcType=TIMESTAMP}", "SYSDATE"));
                element.setText(element.getText().replace("#{isDeleted,jdbcType=DECIMAL}", "0"));
                element.setText(element.getText().replace("#{version,jdbcType=DECIMAL}", "0"));
            }
            if("updateByPrimaryKey".equals(attribute.getValue())) {
                element.setText(element.getText().replace("#{sysUpdateTime,jdbcType=TIMESTAMP}", "SYSDATE"));
                element.setText(element.getText().replace("VERSION = #{version,jdbcType=DECIMAL},", ""));
                //element.setText(element.getText().replace("IS_DELETED = #{isDeleted,jdbcType=DECIMAL}", "IS_DELETED=0"));
                element.setText(element.getText().replace("SYS_CREATE_ID = #{sysCreateId,jdbcType=VARCHAR},",""));
                element.setText(element.getText().replace("SYS_DELETE_ID = #{sysDeleteId,jdbcType=VARCHAR}","VERSION=VERSION+1"));
                element.setText(element.getText().replace("SYS_CREATE_TIME = #{sysCreateTime,jdbcType=TIMESTAMP},",""));
                element.setText(element.getText().replace("SYS_DELETE_TIME = #{sysDeleteTime,jdbcType=TIMESTAMP},",""));
            }
            if("updateByPrimaryKeySelective".equals(attribute.getValue())) {
                List<Element> elementSetList = element.elements();
                for (Element elementset:elementSetList){
                    List<Element> elementIfList = elementset.elements();
                    for (Element elementIf:elementIfList){
                        //<if test="version != null">VERSION = #{version,jdbcType=DECIMAL},</if>
                        if("version != null".equals(elementIf.attribute("test").getValue())){
                            elementIf.setText("VERSION=VERSION+1,");
                        }
                    }
                }
            }
        }
        //输出格式
        OutputFormat format = OutputFormat.createPrettyPrint();
        //设置编码
        format.setEncoding("UTF-8");
        //XMLWriter 指定输出文件以及格式
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLWriter writer = new XMLWriter(new OutputStreamWriter(baos,"UTF-8"), format);
        //写入新文件
        writer.write(document);
        writer.flush();
        writer.close();
        textArea.setText(baos.toString());
    }
    private String getDtoString(IntrospectedTable introspectedTable) {
        String objectName = introspectedTable.getTableConfiguration().getDomainObjectName();
        String text="";
        for (GeneratedJavaFile javaFile:generatedJavaFiles){
            if(javaFile.getCompilationUnit().getType().getShortName().replace(tablePreName,"").equals(objectName)){
                text=javaFile.toString();
            }
        }
        return text;
    }
    private String getMapperString(IntrospectedTable introspectedTable) {
        String objectName = introspectedTable.getTableConfiguration().getDomainObjectName();
        String text="";
        for (GeneratedJavaFile javaFile:generatedJavaFiles){
            if(javaFile.getCompilationUnit().getType().getShortName().replace(tablePreName,"").equals(objectName+"Mapper")){
                text=javaFile.toString();
            }
        }
        return text;
    }
    private String getMapperXMLString(IntrospectedTable introspectedTable) {
        String objectName = introspectedTable.getTableConfiguration().getDomainObjectName();
        String text="";
        for (GeneratedXmlFile javaFile:generatedXmlFiles){
            if(javaFile.getFileName().replace(tablePreName,"").equals(objectName+"Mapper.xml")){
                text=javaFile.toString();
            }
        }
        return text;
    }

    /**
     * 获取ModelJS字符串
     * @param introspectedTable
     * @return
     */
    private String getModelString(IntrospectedTable introspectedTable) {
        String objectName = introspectedTable.getTableConfiguration().getDomainObjectName();
        String text="Ext.define('"+packagePrefix+".model."+objectName+"Model', {\n" +
                "\textend: 'Ext.data.Model',\n"+
                "\talias: 'model."+objectName+"',\n";
        if(introspectedTable.getPrimaryKeyColumns().size()>0) {
            text+="\tidProperty: '" + introspectedTable.getPrimaryKeyColumns().get(0).getJavaProperty() + "',\n";
        }
        text+="\tfields: [\n";
        for (IntrospectedColumn columnOverride:introspectedTable.getAllColumns()) {
            String type = columnOverride.getFullyQualifiedJavaType().getShortName().toLowerCase();
            String property = columnOverride.getJavaProperty();
            if (type.equals("date")) {
                text += "\t\t{name: '" + property + "',mapping: '" + property + "',  type: '" + type + "',dateFormat:'Y-m-d H:i:s'},\n";
            } else if (type.equals("integer")) {
                text += "\t\t{name: '" + property + "',mapping: '" + property + "',  type: 'int'},\n";
            } else if (type.equals("bigdecimal")||type.equals("double")||type.equals("short")) {
                text += "\t\t{name: '" + property + "',mapping: '" + property + "',  type: 'number'},\n";
            } else if (type.equals("boolean")) {
                text += "\t\t{name: '" + property + "',mapping: '" + property + "',  type: 'boolean', defaultValue: false},\n";
            } else {
                text += "\t\t{name: '" + property + "',mapping: '" + property + "',  type: 'string'},\n";
            }
        }
        text+="\t]\n" +
                "});";
        return text;
    }

    /**
     * 获取GridColumnsStringJS字符串
     * @param introspectedTable
     * @return
     */
    private String getGridColumnsString(IntrospectedTable introspectedTable) {
        String objectName = introspectedTable.getTableConfiguration().getDomainObjectName();
        String text="\tcolumns: [\n";
        for (IntrospectedColumn columnOverride:introspectedTable.getAllColumns()) {
            String type = columnOverride.getFullyQualifiedJavaType().getShortName().toLowerCase();
            String property = columnOverride.getJavaProperty();
            String remarks=columnOverride.getRemarks();
            if (type.equals("date")) {
                text += "\t\t{text: '"+remarks+"', dataIndex: '"+property+"',flex:1, xtype: 'datecolumn',format: 'Y-m-d H:i:s'},\n";
            } else if (type.equals("integer")) {
                text += "\t\t{text: '" + remarks + "',dataIndex: '" + property + "',flex:1,xtype: 'numbercolumn',align:'right',},\n";
            }else if (type.equals("bigdecimal")||type.equals("double")) {
                text += "\t\t{text: '" + remarks + "',dataIndex: '" + property + "',flex:1,xtype: 'numbercolumn', format:'0.00',align:'right',},\n";
            } else {
                text += "\t\t{text: '" + remarks + "',dataIndex: '" + property + "',flex:1},\n";
            }
        }
        text+="\t]\n";
        return text;
    }
    private void initCfgXML(String fileName) throws IOException, XMLParserException, SQLException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        //初始化数据
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream(fileName);
        List<String> warnings = new ArrayList();
        ConfigurationParser cp = new ConfigurationParser(warnings);
        Configuration config = cp.parseConfiguration(is);
        List<Context> contextList=config.getContexts();
        map.clear();
        comboBoxTable.removeAllItems();
        ProgressCallback callback = new NullProgressCallback();
        generatedJavaFiles.clear();
        generatedXmlFiles.clear();
        for (Context context:contextList){
            PluginConfiguration pluginConfiguration=new PluginConfiguration();
            pluginConfiguration.setConfigurationType("com.funi.demo.DemoPlugin");
            context.addPluginConfiguration(pluginConfiguration);
            CommentGeneratorConfiguration commentGeneratorConfiguration=new CommentGeneratorConfiguration();
            commentGeneratorConfiguration.setConfigurationType("com.funi.demo.CommentGenerator");
            context.setCommentGeneratorConfiguration(commentGeneratorConfiguration);
            context.addProperty("suppressDate","true");
            tablePreName=context.getProperty("tablePreName");
            context.introspectTables(callback, warnings, null);
            Field file=context.getClass().getDeclaredField("introspectedTables");
            file.setAccessible(true);
            List<IntrospectedTable> list=(List<IntrospectedTable>)file.get(context);
            for (IntrospectedTable introspectedTable:list){
                String tableName=introspectedTable.getTableConfiguration().getTableName();
                System.out.println(tablePreName);
                TableConfiguration tableConfiguration=introspectedTable.getTableConfiguration();
                System.out.println(tableConfiguration.getTableName());
                System.out.println(tableConfiguration.getDomainObjectName());
                String domainObjectName=JavaBeansUtil.getCamelCaseString(tableConfiguration.getTableName(),true).replace(tablePreName,"");
                System.out.println(domainObjectName);
                introspectedTable.getTableConfiguration().setDomainObjectName(domainObjectName);
                introspectedTable.getTableConfiguration().setCountByExampleStatementEnabled(false);
                introspectedTable.getTableConfiguration().setDeleteByExampleStatementEnabled(false);
                introspectedTable.getTableConfiguration().setSelectByExampleStatementEnabled(false);
                introspectedTable.getTableConfiguration().setUpdateByExampleStatementEnabled(false);
                //<columnOverride column="IS_DELETED" javaType="java.lang.Boolean"/>
                ColumnOverride columnOverride=new ColumnOverride("IS_DELETED");
                columnOverride.setJavaType("java.lang.Boolean");
                introspectedTable.getTableConfiguration().addColumnOverride(columnOverride);
                comboBoxTable.addItem(tableName);
                map.put(tableName,introspectedTable);
            }
            context.generateFiles(callback, generatedJavaFiles,
                    generatedXmlFiles, warnings);
        }
    }

}
