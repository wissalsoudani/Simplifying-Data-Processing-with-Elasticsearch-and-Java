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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class XmlToElasticsearch {
// choisir un index qui va etre creer lors du parsing
    private static final String INDEX_NAME = "products";

    public static void main(String[] args) {
        try (RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost("localhost", 9200, "http")))) {

            // Décompresser et analyser le fichier XML dans l'archive ZIP
            parseAndIndexXmlFromZip(client, "./xml.zip");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void parseAndIndexXmlFromZip(RestHighLevelClient client, String zipFilePath) throws Exception {
        try (InputStream fis = new FileInputStream(zipFilePath);
             ZipInputStream zis = new ZipInputStream(fis)) {
    
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                System.out.println("Processing entry: " + entry.getName());
    
                if (entry.getName().endsWith(".xml")) {
                    System.out.println("Processing file: " + entry.getName());
    
                    // Create a temporary input stream for the current entry
                    try (InputStream entryStream = new BufferedInputStream(zis)) {
                        parseAndIndexXml(client, entryStream);
                    }
    
                    // Explicitly close the current ZIP entry
                    zis.closeEntry();
                    System.out.println("Entry closed successfully.");
                }
            }
        } catch (IOException e) {
            System.err.println("Error processing ZIP file: " + e.getMessage());
            throw e;
        }
    }
    

private static void parseAndIndexXml(RestHighLevelClient client, InputStream xmlInputStream) throws Exception {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    SAXParser saxParser = factory.newSAXParser();

    // Use a SAX handler to process the XML file
    DefaultHandler handler = new DefaultHandler() {
        Map<String, String> productData = new HashMap<>();
        StringBuilder content = new StringBuilder();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            content.setLength(0); // Reset content buffer
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if ("product".equalsIgnoreCase(qName)) {
                // Clean and send product data to Elasticsearch
                cleanProductData(productData);
                sendToElasticsearch(client, productData);

                productData.clear(); // Reset for the next product
            } else {
                productData.put(qName, content.toString().trim());
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            content.append(ch, start, length);
        }
    };

    // Parse the XML stream
    saxParser.parse(xmlInputStream, handler);
}


    private static void cleanProductData(Map<String, String> productData) {
        // Nettoyer les balises HTML et décoder les entités HTML pour la propreté des données envoyés
        productData.replaceAll((key, value) -> {
            if (value != null) {
                // Supprimer les balises HTML
                value = value.replaceAll("\\<.*?\\>", "").trim();
                // Décoder les entités HTML
                value = value.replace("&nbsp;", " ")
                             .replace("&eacute;", "é")
                             .replace("&agrave;", "à")
                             .replace("&egrave;", "è")
                             .replace("&amp;", "&")
                             .replace("&quot;", "\"")
                             .replace("&lt;", "<")
                             .replace("&gt;", ">");
                return value;
            }
            return null;
        });
    
        // Harmoniser les formats pour certains champs
        if (productData.containsKey("it_display_size_pouces")) {
            productData.put("it_display_size_pouces", productData.get("it_display_size_pouces").replace(",", "."));
        }
    
        // Supprimer les champs inutiles ou non pertinents si nécessaire
        productData.remove("warranty_scope"); // Exemple de suppression de champ
    }
    

    private static void sendToElasticsearch(RestHighLevelClient client, Map<String, String> productData) {
        try {
            BulkRequest bulkRequest = new BulkRequest();
            bulkRequest.add(new IndexRequest(INDEX_NAME).source(productData, XContentType.JSON));

            // Envoyer les data à Elasticsearch
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
