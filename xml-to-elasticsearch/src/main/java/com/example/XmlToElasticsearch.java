package com.example;

import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class XmlToElasticsearch {

    private static final String INDEX_NAME = "products";

    public static void main(String[] args) {
        try (RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost("localhost", 9200, "http")))) {

            // Décompresser et analyser le fichier XML dans l'archive ZIP
            parseAndIndexXmlFromZip(client, "xml.zip");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void parseAndIndexXmlFromZip(RestHighLevelClient client, String zipFilePath) throws Exception {
        // Ouvrir l'archive ZIP
        try (InputStream fis = new FileInputStream(zipFilePath);
             ZipInputStream zis = new ZipInputStream(fis)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".xml")) {
                    // Analyser chaque fichier XML dans l'archive
                    parseAndIndexXml(client, zis);
                }
            }
        }
    }

    private static void parseAndIndexXml(RestHighLevelClient client, InputStream xmlInputStream) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();

        // Handler SAX pour traiter chaque balise <product>
        saxParser.parse(xmlInputStream, new DefaultHandler() {
            Map<String, String> productData = new HashMap<>();
            StringBuilder content = new StringBuilder();

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                content.setLength(0); // Réinitialiser le contenu
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                if ("product".equalsIgnoreCase(qName)) {
                    // Envoi du produit à Elasticsearch
                    sendToElasticsearch(client, productData);
                    productData.clear();
                } else {
                    productData.put(qName, content.toString().trim());
                }
            }

            @Override
            public void characters(char[] ch, int start, int length) throws SAXException {
                content.append(ch, start, length);
            }
        });
    }

    private static void sendToElasticsearch(RestHighLevelClient client, Map<String, String> productData) {
        try {
            BulkRequest bulkRequest = new BulkRequest();
            bulkRequest.add(new IndexRequest(INDEX_NAME).source(productData, XContentType.JSON));

            // Envoi à Elasticsearch
            BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (response.hasFailures()) {
                System.err.println("Erreur d'indexation : " + response.buildFailureMessage());
            } else {
                System.out.println("Produit indexé : " + productData.get("article_sku"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
