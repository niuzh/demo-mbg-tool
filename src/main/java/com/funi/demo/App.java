package com.funi.demo;

import org.mybatis.generator.exception.XMLParserException;

import java.io.IOException;

/**
 * 启动窗口
 *
 */
public class App 
{
    public static void main( String[] args ) throws IOException, XMLParserException {
        CodeGeneratorForm from=new CodeGeneratorForm();
        from.pack();
        from.setSize(1280, 600);
        from.setLocation(10,10);
        from.setVisible(true);
    }
}
