package org.opendatanode.plugins.extractor.sk.judge;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;

public class JudgeSkCrawler {
    
    private static final Logger LOG = LoggerFactory.getLogger(JudgeSkCrawler.class);

    /**
     * <p>
     * Http connect timeout for request to CKAN.<br/>
     * Limits how long it tries to connect to CKAN, not how long the request can take.
     * </p>
     * 
     * <p>unit: miliseconds</p>
     */
    private static final int CONNECT_TIMEOUT = 10000;
    private static final String PARAM_MENO          = "ctl00$ctl00$PlaceHolderMain$PlaceHolderMain$ctl01$txtMeno";
    private static final String PARAM_SUD           = "ctl00$ctl00$PlaceHolderMain$PlaceHolderMain$ctl01$cmbSud";
    private static final String PARAM_FUNKCIA       = "ctl00$ctl00$PlaceHolderMain$PlaceHolderMain$ctl01$cmbFunkcia";
    private static final String PARAM_PAGE          = "ctl00$ctl00$PlaceHolderMain$PlaceHolderMain$ctl01$gvSudcaZoznam$ctl13$ctl00$cmbAGVPager";
    private static final String PARAM_ROWS_PER_PAGE = "ctl00$ctl00$PlaceHolderMain$PlaceHolderMain$ctl01$gvSudcaZoznam$ctl13$ctl00$cmbAGVCountOnPage";

    /**
     * Parses judges from given url. Recursively goes through all pages
     * 
     * @param url
     * @param page
     * @param judges
     * @param params
     * @throws Exception
     */
    public static void getJudgesPost(String url, int page, final List<Judge> judges, List<NameValuePair> params) throws Exception {
        LOG.debug("Loading page " + page);
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        FileOutputStream fos = null;

        Map<String, String> additionalHttpHeaders = new HashMap<String, String>();
        additionalHttpHeaders.put("Content-Type", "application/x-www-form-urlencoded");
        Parameters retVal;

        try {
            URIBuilder uriBuilder = new URIBuilder(url);
            uriBuilder.setPath(uriBuilder.getPath());
            HttpPost httpPost = new HttpPost(uriBuilder.build().normalize());
            for (Map.Entry<String, String> additionalHeader : additionalHttpHeaders.entrySet()) {
                httpPost.addHeader(additionalHeader.getKey(), additionalHeader.getValue());
            }
            httpPost.setConfig(RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT).build());

            params.add(new BasicNameValuePair(PARAM_MENO, ""));
            params.add(new BasicNameValuePair(PARAM_SUD, "0"));
            params.add(new BasicNameValuePair(PARAM_FUNKCIA, "0"));
            params.add(new BasicNameValuePair(PARAM_PAGE, String.valueOf(page)));
            params.add(new BasicNameValuePair(PARAM_ROWS_PER_PAGE, "10"));
            
            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            response = client.execute(httpPost);

            StringWriter writer = new StringWriter();
            IOUtils.copy(response.getEntity().getContent(), writer);
            
            // retVal holds parameters necessary for POSTing request for next page
            retVal = addJusdgesFromXml(parseFormOnly(writer.toString()), judges);
        } finally {
            tryCloseHttpResponse(response);
            tryCloseHttpClient(client);
            if (fos != null) {
                fos.close();
            }
        }
        
        if (page >= retVal.pages) {
            return;
        }
        // next page
        getJudgesPost(url, page + 1, judges, retVal.requiredParams);
    }
    
    /**
     * Parses through xpath all judges in given html (1 page of judges)
     * 
     * @param html
     * @param judges
     * @return
     */
    private static Parameters addJusdgesFromXml(String html, List<Judge> judges) {
        Document doc;
        try {
            
            InputStream is = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8));
            Tidy t = new Tidy();
            t.setInputEncoding("UTF-8");
            t.setXHTML(true);
            t.setShowWarnings(false);
            t.setShowErrors(0);
            t.setQuiet(true);
            doc = t.parseDOM(is, null);
            
            Parameters parameters = new Parameters();
            parameters.requiredParams = parseHiddenParameters(doc);
            parameters.pages = Integer.parseInt(getString(doc, "//select[contains(@name,'cmbAGVPager')]/option[last()]/text()"));
            
            NodeList nodeList = getNodes(doc, "//tr");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node item = nodeList.item(i);
                
                boolean aktive = "Aktívny".equals(getString(item, "td/img/@title"));
                String meno = getString(item, "td[@title='Meno']/text()");
                String funkcia = getString(item, "td[@title='Funkcia']/text()");
                String sud = getString(item, "td[@title='Súd']/text()");
                String poznamka = getString(item, "td[@title='Poznámka']/text()");
                
                try {
                    judges.add(new Judge(aktive, meno, funkcia, sud, poznamka));
                } catch (Exception e) {
                    // skip: headers or paging ...
                }
            }
            
            return parameters;
        } catch (XPathExpressionException e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * The least hidden parameters, needed to go through paging.
     * Without them POSTing the form wouldn't return next page
     * 
     * @param doc
     * @return
     * @throws XPathExpressionException
     */
    private static List<NameValuePair> parseHiddenParameters(Document doc) throws XPathExpressionException {
        List<NameValuePair> hiddenParameters = new ArrayList<NameValuePair>();
        
        String viewstate = getString(doc, "//form/input[@id='__VIEWSTATE']/@value");
        String eventval = getString(doc, "//form/input[@id='__EVENTVALIDATION']/@value");
        
        hiddenParameters.add(new BasicNameValuePair("__REQUESTDIGEST", ""));
        hiddenParameters.add(new BasicNameValuePair("__VIEWSTATE", viewstate));
        hiddenParameters.add(new BasicNameValuePair("__VIEWSTATEENCRYPTED", ""));
        hiddenParameters.add(new BasicNameValuePair("__EVENTVALIDATION", eventval));
        
        return hiddenParameters;
    }
    
    private static NodeList getNodes(Object item, String xPathString) throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        return (NodeList) xPath.compile(xPathString).evaluate(item, XPathConstants.NODESET);
    }
    
    private static String getString(Node item, String xPathString) throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        return (String) xPath.compile(xPathString).evaluate(item, XPathConstants.STRING);
    }

    /**
     * The judge html is a mess, even with jTidy there were too many errors to parse is as XML.
     * Therefore only the formular is parsed from the page first
     * 
     * @param html
     * @return
     */
    private static String parseFormOnly(String html) {
        Pattern p = Pattern.compile("(<form.*form>)", Pattern.DOTALL | Pattern.MULTILINE);
        Matcher m = p.matcher(html);
        
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private static void tryCloseHttpClient(CloseableHttpClient client) {
        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                LOG.warn("Failed to close HTTP client", e);
            }
        }
    }
    
    private static void tryCloseHttpResponse(CloseableHttpResponse response) {
        if (response != null) {
            try {
                response.close();
            } catch (IOException e) {
                LOG.warn("Failed to close HTTP response", e);
            }
        }
    }
}
