# Adelan Test Recruitment Java Program
🚀 **Program developed by Wissal Soudani for the technical recruitment test.**

---

## 📋 Project Description
This program is an efficient solution to parse and index large XML files into Elasticsearch while adhering to memory and performance constraints.

### Key Features:
- **Extraction and Decompression**:
  - The program opens a ZIP archive containing XML files.
  - Only relevant XML files are read and processed.
  
- **Efficient Parsing with SAX**:
  - The SAX parser processes XML files event-by-event, minimizing memory usage.
  - Each `<product>` element is processed individually, avoiding loading the entire XML file into memory.
  
- **Data Cleaning**:
  - Removes HTML tags and decodes HTML entities to ensure clean and consistent data.
  - Harmonizes specific fields, such as converting screen sizes to a standard format.
  
- **Indexing in Elasticsearch**:
  - Each product is directly sent to an Elasticsearch index named `products`.
  - Bulk indexing is used to improve performance when handling large data volumes.

- **Result Visualization**:
  - Results can be checked using Kibana or Postman.
  - For quick testing, a `GET` request was sent via Postman:
    ```http
    GET http://localhost:9200/products/_search?pretty
    ```
    This request retrieves indexed documents in JSON format directly from Elasticsearch.

---

## 🛠️ Code Architecture
The project is structured around the following concepts:

### **Main**
- Initializes the Elasticsearch client.
- Calls the method to extract and process the XML files.

### **Extraction and Parsing**
- **`parseAndIndexXmlFromZip` method**: Unzips the archive and processes each XML file individually.
- **`parseAndIndexXml` method**: Uses the SAX parser to analyze `<product>` tags.

### **Data Cleaning**
- **`cleanProductData` method**: Removes unnecessary data and cleans fields for Elasticsearch indexing.

### **Indexing in Elasticsearch**
- **`sendToElasticsearch` method**: Sends each product to the Elasticsearch server using the Java High-Level REST Client.

---

## ⚙️ Requirements
To run this program, you need:
1. **Java 11**  
2. **Elasticsearch** (version 7.4.1) running on `localhost:9200`.  
3. A **ZIP archive** containing one or more valid XML files must be added to the root of the project.  
4. **Postman** or **Kibana** (optional) to visualize indexed data.

---

## 🚀 Installation Instructions
1. Clone this repository to your machine:
   ```bash
   git clone https://github.com/username/Adelan_test_recruitment_java_program.git
2. Ensure Elasticsearch is running on localhost:9200.
3. Place a file named xml.zip (containing your XML files) in the project directory.
4. Compile and run the program using Maven or your favorite IDE:
   ```bash
   mvn clean install
   java -jar target/Adelan_test_recruitment_java_program.jar (RUN)
5. Visualize the results in Postman by sending the following request:
   ```bash
   GET http://localhost:9200/products/_search?pretty
This will display the data in a readable JSON format directly from Elasticsearch.

---

## 🧹 Highlights
- Memory Optimization: The SAX processing ensures large XML files are handled without memory overload.
- Advanced Data Cleaning: The program ensures the indexed data is clean and ready for Elasticsearch.
- Scalability: Designed to handle massive data volumes through stream-based processing and the Elasticsearch Bulk API.
- Quick Visualization: Indexed results can be quickly verified using Kibana or Postman.

---

## 🔍 Example Output
When the program runs successfully, you will see the following in the console: <br/>
![image](https://github.com/user-attachments/assets/2d742bea-3a83-4a12-b459-8a915e443b68)

In Postman, you can view the indexed data with the GET request:
![image](https://github.com/user-attachments/assets/5b4d89e8-6d31-4582-86bd-4f719897680c)

## 🛠 Troubleshooting
**Issue:**    *java.io.IOException: Stream closed*  <br/>
During testing, the program occasionally throws the following exception:
 `< java.io.IOException: Stream closed
  at java.util.zip.ZipInputStream.ensureOpen>`

**Analysis:**   <br/>
This error occurs when the ZipInputStream is closed prematurely while iterating over entries in the ZIP file. After processing the first XML entry, attempting to move to the next entry results in the exception.
Key points observed:
  - The XML file inside the ZIP contains multiple <product> elements, but only one XML file is 
    present in the archive.
- The issue might stem from improper handling of the input stream or an edge case in the ZipInputStream lifecycle.

**Resolving Attempts**
1. Stream Handling: Ensured the ZipInputStream and entries were properly closed using zis.closeEntry() after processing each entry.
2. Stream Resetting: Verified that the SAX parser correctly processes the XML data without keeping the stream open longer than necessary.
3. Logging: Added logs to track when streams are opened and closed to identify potential mismanagement.

Despite these efforts, the issue persists intermittently when working with ZIP files containing a single XML entry.
![image](https://github.com/user-attachments/assets/a9270265-c51d-4dbb-a464-0204d8695967)
