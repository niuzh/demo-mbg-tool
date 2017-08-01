package com.funi.demo;

import org.mybatis.generator.api.*;
import org.mybatis.generator.config.*;
import org.mybatis.generator.config.xml.ConfigurationParser;
import org.mybatis.generator.exception.XMLParserException;
import org.mybatis.generator.internal.NullProgressCallback;
import org.mybatis.generator.internal.util.JavaBeansUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                    objectName+="";
                }
                if("Mapper".equals(selectedItem)){
                    text += getMapperString(introspectedTable);
                    text=text.replace(tablePreName+objectName,objectName);
                    objectName+="Mapper";
                }
                if("Mapper.XML".equals(selectedItem)){
                    text = getMapperXMLString(introspectedTable);
                    text=text.replace(tablePreName+objectName,objectName);
                    objectName+="Mapper.xml";
                }
                if("ExtJSModel".equals(selectedItem)){
                    text += getModelString(introspectedTable);
                    text=text.replace(tablePreName+objectName,objectName);
                    objectName+="Model";
                }
                System.out.println(tablePreName+objectName);
                text=text.replace(tablePreName+objectName,objectName);
                textArea.setText(text);
                textFieldFileName.setText(objectName);
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
    }
    private String getDtoString(IntrospectedTable introspectedTable) {
        String objectName = introspectedTable.getTableConfiguration().getDomainObjectName();
        String text="";
        for (GeneratedJavaFile javaFile:generatedJavaFiles){
            if(javaFile.getCompilationUnit().getType().getShortName().endsWith(objectName)){
                text=javaFile.toString();
            }
        }
        return text;
    }
    private String getMapperString(IntrospectedTable introspectedTable) {
        String objectName = introspectedTable.getTableConfiguration().getDomainObjectName();
        String text="";
        for (GeneratedJavaFile javaFile:generatedJavaFiles){
            if(javaFile.getCompilationUnit().getType().getShortName().endsWith(objectName+"Mapper")){
                text=javaFile.toString();
            }
        }
        return text;
    }
    private String getMapperXMLString(IntrospectedTable introspectedTable) {
        String objectName = introspectedTable.getTableConfiguration().getDomainObjectName();
        String text="";
        for (GeneratedXmlFile javaFile:generatedXmlFiles){
            if(javaFile.getFileName().endsWith(objectName+"Mapper.xml")){
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
            }else if (type.equals("bigdecimal")||type.equals("double")) {
                text += "\t\t{name: '" + property + "',mapping: '" + property + "',  type: 'number'},\n";
            } else {
                text += "\t\t{name: '" + property + "',mapping: '" + property + "',  type: '" + type + "'},\n";
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
                comboBoxTable.addItem(tableName);
                map.put(tableName,introspectedTable);
            }
            context.generateFiles(callback, generatedJavaFiles,
                    generatedXmlFiles, warnings);
        }
    }

}
