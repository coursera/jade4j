package de.neuland.jade4j;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import de.neuland.jade4j.Jade4J.Mode;
import de.neuland.jade4j.exceptions.JadeCompilerException;
import de.neuland.jade4j.exceptions.JadeException;
import de.neuland.jade4j.expression.ExpressionHandler;
import de.neuland.jade4j.filter.*;
import de.neuland.jade4j.model.JadeModel;
import de.neuland.jade4j.parser.Parser;
import de.neuland.jade4j.parser.node.Node;
import de.neuland.jade4j.template.FileTemplateLoader;
import de.neuland.jade4j.template.JadeTemplate;
import de.neuland.jade4j.template.TemplateLoader;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class JadeConfiguration {

    private static final String FILTER_CDATA = "cdata";
    private static final String FILTER_STYLE = "css";
    private static final String FILTER_SCRIPT = "js";

    private boolean prettyPrint = false;
    private boolean caching = true;
    private Mode mode = Jade4J.Mode.HTML;

    private Map<String, Filter> filters = new HashMap<String, Filter>();
    private Map<String, Object> sharedVariables = new HashMap<String, Object>();
    private TemplateLoader templateLoader = new FileTemplateLoader("", "UTF-8");
    protected static final int MAX_ENTRIES = 1000;

    public JadeConfiguration() {
        setFilter(FILTER_CDATA, new CDATAFilter());
        setFilter(FILTER_SCRIPT, new JsFilter());
        setFilter(FILTER_STYLE, new CssFilter());
    }

    private Map<String, JadeTemplate> cache = new ConcurrentLinkedHashMap.Builder<String, JadeTemplate>().maximumWeightedCapacity(
            MAX_ENTRIES + 1).build();

    public JadeTemplate getTemplate(String name) throws IOException, JadeException {
        return getTemplate(name, null, Collections.EMPTY_MAP);
    }

    /**
     * @param name: template name
     * @param languageCode: the language that provided template will be translated to. There is no restriction
     *                    on how language code should look like since it is only used for differentiate templates on caching.
     *                    Your code base should be consistent with the format of language code that is passed in to reduce the rendering
     *                    speed.
     * @param originalToTranslated: a mapping between original string to translated string.
     * @return JadeTemplate
     * @throws IOException
     * @throws JadeException
     */
    public JadeTemplate getTemplate(String name, String languageCode, Map<String, String> originalToTranslated) throws IOException, JadeException {
        if (caching) {
            long lastModified = templateLoader.getLastModified(name);
            String key;
            if (languageCode != null) {
                key = name + "-" + languageCode + "-" + lastModified;
            } else {
                key = name + "--" + lastModified;
            }
            JadeTemplate template = cache.get(key);

            if (template != null) {
                return template;
            } else {
                JadeTemplate newTemplate = createTemplate(name, languageCode, originalToTranslated);
                cache.put(key, newTemplate);
                return newTemplate;
            }
        }

        return createTemplate(name, languageCode, originalToTranslated);
    }

    public void renderTemplate(JadeTemplate template, Map<String, Object> model, Writer writer) throws JadeCompilerException {
        JadeModel jadeModel = new JadeModel(sharedVariables);
        for (String filterName : filters.keySet()) {
            jadeModel.addFilter(filterName, filters.get(filterName));
        }
        jadeModel.putAll(model);
        template.process(jadeModel, writer);
    }

    public String renderTemplate(JadeTemplate template, Map<String, Object> model) throws JadeCompilerException {
        StringWriter writer = new StringWriter();
        renderTemplate(template, model, writer);
        return writer.toString();
    }

    private JadeTemplate createTemplate(String name, String languageCode, Map<String, String> originalToTranslated) throws JadeException, IOException {
        JadeTemplate template = new JadeTemplate();

        Parser parser = new Parser(name, templateLoader, languageCode, originalToTranslated);
        Node root = parser.parse();
        template.setTemplateLoader(templateLoader);
        template.setRootNode(root);
        template.setPrettyPrint(prettyPrint);
        template.setMode(getMode());
        return template;
    }

    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    public void setFilter(String name, Filter filter) {
        filters.put(name, filter);
    }

    public void removeFilter(String name) {
        filters.remove(name);
    }

    public Map<String, Object> getSharedVariables() {
        return sharedVariables;
    }

    public void setSharedVariables(Map<String, Object> sharedVariables) {
        this.sharedVariables = sharedVariables;
    }

    public TemplateLoader getTemplateLoader() {
        return templateLoader;
    }

    public void setTemplateLoader(TemplateLoader templateLoader) {
        this.templateLoader = templateLoader;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public boolean templateExists(String url) {
        try {
            return templateLoader.getReader(url) != null;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean isCaching() {
        return caching;
    }

    public void setCaching(boolean cache) {
        if (cache != this.caching) {
            ExpressionHandler.setCache(cache);
            this.caching = cache;
        }
    }

    public void clearCache() {
        ExpressionHandler.clearCache();
        cache.clear();
    }
}
