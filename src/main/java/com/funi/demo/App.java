package com.funi.demo;

import org.mybatis.generator.exception.XMLParserException;

import java.awt.EventQueue;
import java.io.IOException;

/**
 * 启动窗口
 *
 */
public class App {
	public static void main(String[] args){
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				CodeGeneratorForm from;
				try {
					from = new CodeGeneratorForm();
					from.pack();
					from.setSize(1280, 600);
					from.setLocation(10, 10);
					from.setVisible(true);
				} catch (IOException | XMLParserException e) {
					e.printStackTrace();
				}
			}
		});

	}
}
