package com.funi.demo;

import org.mybatis.generator.api.GeneratedJavaFile;
import org.mybatis.generator.api.GeneratedXmlFile;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.ProgressCallback;
import org.mybatis.generator.config.CommentGeneratorConfiguration;
import org.mybatis.generator.config.Configuration;
import org.mybatis.generator.config.Context;
import org.mybatis.generator.config.PluginConfiguration;
import org.mybatis.generator.config.xml.ConfigurationParser;
import org.mybatis.generator.exception.XMLParserException;
import org.mybatis.generator.internal.NullProgressCallback;

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
    private static final String packagePrefix="demo";
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
        comboBoxCode.addItem("Example");
        comboBoxCode.addItem("Dto");
        comboBoxCode.addItem("Mapper");
        comboBoxCode.addItem("Mapper.XML");
        comboBoxCode.addItem("ExtJSModel");
        comboBoxCode.addItem("ExtJSStore");
        comboBoxCode.addItem("ExtJSGridColumns");
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
                /*if("Dto".equals(selectedItem)){
                    text += getDtoString(introspectedTable);
                    objectName+="";
                }
                if("Mapper".equals(selectedItem)){
                    text += getMapperString(introspectedTable);
                    objectName+="Mapper";
                }
                if("Mapper.XML".equals(selectedItem)){
                    text = getMapperXMLString(introspectedTable);
                    objectName+="Mapper.xml";
                }
                if("ExtJSModel".equals(selectedItem)){
                    text += getModelString(introspectedTable);
                    objectName+="Model";
                }*/
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
            context.introspectTables(callback, warnings, null);
            Field file=context.getClass().getDeclaredField("introspectedTables");
            file.setAccessible(true);
            List<IntrospectedTable> list=(List<IntrospectedTable>)file.get(context);
            for (IntrospectedTable introspectedTable:list){
                String tableName=introspectedTable.getTableConfiguration().getTableName();
                comboBoxTable.addItem(tableName);
                map.put(tableName,introspectedTable);
            }
            context.generateFiles(callback, generatedJavaFiles,
                    generatedXmlFiles, warnings);
        }
    }

}
