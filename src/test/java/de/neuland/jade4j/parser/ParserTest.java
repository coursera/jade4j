package de.neuland.jade4j.parser;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.neuland.jade4j.parser.node.BlockNode;
import de.neuland.jade4j.parser.node.TagNode;
import org.junit.Assert;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import de.neuland.jade4j.TestFileHelper;
import de.neuland.jade4j.parser.node.Node;
import de.neuland.jade4j.template.FileTemplateLoader;

public class ParserTest {

	protected Parser parser;
	protected Node root;

	protected void loadInParser(String fileName) {

		try {
			FileTemplateLoader loader = new FileTemplateLoader(
					TestFileHelper.getParserResourcePath(""), "UTF-8");
			parser = new Parser(fileName, loader);
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail("template " + fileName + " was not found");
		}
		root = parser.parse();
	}
}
