package org.expeditee.greenstone;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class provides a simple API for communicating with a Greenstone 3 server
 * using SOAP.
 * <p>
 * Greenstone 3 does not yet 'properly' implement SOAP-based web services. We
 * would like to use a Greenstone WSDL (Web Services Definition Language) file
 * and a higher level SOAP Client interface. But we can't. To get around this,
 * this API uses a simple socket connection to the Greenstone 3 server, and
 * sends SOAP requests as strings (XML documents). This works but isn't elegant.
 * The server responds with a string representing an XML document.
 * <p>
 * The server's hostname and port are hard-coded. <b>Do not modify them.</b>
 * <p>
 * The Greenstone collection to use is <i>hcibib</i>, and this is also
 * hard-coded. <b>Do not modify this.</b>
 * <p>
 * This collection can be accessed from a web browser at <a
 * href="http://delaware.resnet.scms.waikato.ac.nz:8111/greenstone3/library?a=p&sa=about&c=hcibib">
 * this location</a>.
 */
public class Greenstone3Connection {
	/** an ordered list of {@link Query} objects */
	private List<Query> queryList;

	/**
	 * a HashMap of {@link ResultDocument} objects with document IDs as the
	 * keys. All the results returned in this session.
	 */
	private Map<String, ResultDocument> allResults;

	/**
	 * a HashMap keyed on the keywords found for all documents returned in this
	 * session. Each item in the map is itself a HashMap, keyed on document IDs
	 * with each item being NULL.
	 */
	private Map<String, Set<String>> allKeywords;

	/**
	 * a set of authors names
	 */
	private Map<String, Set<String>> allAuthors;

	/**
	 * a HashMap keyed on the publication dates found for all documents returned
	 * in this session. Each item in the map is itself a HashMap, keyed on
	 * document IDs with each item being NULL.
	 */
	private Map<String, Set<String>> allDates;

	/**
	 * a HashMap keyed on the journal names found for all documents returned in
	 * this session. Each item in the map is itself a HashMap, keyed on document
	 * IDs with each item being NULL.
	 */
	private Map<String, Set<String>> allJournals;

	/**
	 * a HashMap keyed on the book titles found for all documents returned in
	 * this session. Each item in the map is itself a HashMap, keyed on document
	 * IDs with each item being NULL.
	 */
	private Map<String, Set<String>> allBooktitles;

	/** the <i>hostname</i> where the Greenstone 3 server is running */
	private String hostname;

	/** the <i>port</i> on which the Greenstone 3 server is running */
	private int port;

	/** for communication with the server */
	private Socket socket = null;

	/** for writing the SOAP request strings to the server socket */
	private PrintWriter toGSDL = null;

	/** for reading the SOAP response strings from the server socket */
	private BufferedReader fromGSDL = null;

	/** string that starts every SOAP request */
	private String SOAPrequestHeader;

	/** acts as a template for every SOAP request string */
	private String SOAPrequestMessage = "<?xml version='1.0' encoding='UTF-8'?><soapenv:Envelope xmlns:soapenv='http://schemas.xmlsoap.org/soap/envelope/' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'><soapenv:Body><message><request lang='en' to='hcibib/PROCESSNAME' type='PROCESSTYPE'>REQUESTBODY</request></message></soapenv:Body></soapenv:Envelope>";

	/**
	 * A client application using this API will normally only create one
	 * instance of this class.
	 * <p>
	 * Create an instance with something like this
	 * 
	 * <pre>
	 * Greenstone3Connection gsdl = new Greenstone3Connection();
	 * </pre>
	 * 
	 * The constructor initialises the following <b>private</b> variables...
	 * <ul>
	 * <li>the <i>hostname</i> where the Greenstone 3 server is running</li>
	 * <li>the <i>port</i> on which the Greenstone 3 server is running</li>
	 * <li><i>queryList</i> an ordered list of {@link Query} objects</li>
	 * <li><i>allResults</i> a HashMap of {@link ResultDocument} objects with
	 * document IDs as the keys. All the results returned in this session.</li>
	 * <li><i>allKeywords</i> a HashMap keyed on the keywords found for all
	 * documents returned in this session. Each item in the map is itself a
	 * HashMap, keyed on document IDs with each item being NULL.</li>
	 * <li><i>allAuthors</i> a HashMap keyed on the author names found for all
	 * documents returned in this session. Each item in the map is itself a
	 * HashMap, keyed on document IDs with each item being NULL.</li>
	 * <li><i>allDates</i> a HashMap keyed on the publication dates found for
	 * all documents returned in this session. Each item in the map is itself a
	 * HashMap, keyed on document IDs with each item being NULL.</li>
	 * <li><i>allJournals</i> a HashMap keyed on the journal names found for
	 * all documents returned in this session. Each item in the map is itself a
	 * HashMap, keyed on document IDs with each item being NULL.</li>
	 * <li><i>allBooktitles</i> a HashMap keyed on the book titles found for
	 * all documents returned in this session. Each item in the map is itself a
	 * HashMap, keyed on document IDs with each item being NULL.</li>
	 * </ul>
	 */
	public Greenstone3Connection(int location) {
		if (location == 0) {
			this.hostname = "comp537.cs.waikato.ac.nz";
			this.port = 80;
			this.SOAPrequestHeader = "POST /greenstone3/services/localsite HTTP/1.1\nHost: comp537.cs.waikato.ac.nz:80\nSOAPAction: hcibib/PROCESSNAME\nContent-Type: text/xml;charset=utf-8\nContent-Length: ";
		} else {
			this.hostname = "130.217.220.10";
			this.port = 8111;
			this.SOAPrequestHeader = "POST /greenstone3/services/localsite HTTP/1.1\nHost: 130.217.220.10:8111\nSOAPAction: hcibib/PROCESSNAME\nContent-Type: text/xml;charset=utf-8\nContent-Length: ";
		}
		this.queryList = Collections.synchronizedList(new ArrayList<Query>());
		this.allResults = Collections
				.synchronizedMap(new HashMap<String, ResultDocument>());
		this.allKeywords = Collections.synchronizedMap(new HashMap<String, Set<String>>());
		this.allAuthors = Collections
				.synchronizedMap(new HashMap<String, Set<String>>());
		this.allDates = Collections.synchronizedMap(new HashMap<String, Set<String>>());
		this.allJournals = Collections.synchronizedMap(new HashMap<String, Set<String>>());
		this.allBooktitles = Collections.synchronizedMap(new HashMap<String, Set<String>>());
	}

	public Map<String, ResultDocument> getSessionResults() {
		return this.allResults;
	};

	/**
	 * Print a string representation of the list of queries issued in this
	 * session.
	 */
	public void dumpQueryList() {
		ListIterator iter = queryList.listIterator();
		while (iter.hasNext()) {
			Query query = (Query) iter.next();
			System.out.println(query.toString());
		}
	}

	/**
	 * Print a string representation of the Booktitles occuring for all query
	 * results in this session. For each booktitle print the IDs of the
	 * documents with that booktitle.
	 */
	public void dumpAllBooktitles() {
		Set keys = allBooktitles.keySet();
		Iterator iter = keys.iterator();
		while (iter.hasNext()) {
			String booktitle = (String) iter.next();
			HashMap docMap = (HashMap) allBooktitles.get(booktitle);
			System.out.println(booktitle);
			System.out.println(docMap.keySet().toString());
		}
	}

	/**
	 * Print a string representation of the Journals occuring for all query
	 * results in this session. For each journal print the IDs of the documents
	 * with that journal.
	 */
	public void dumpAllJournals() {
		Set keys = allJournals.keySet();
		Iterator iter = keys.iterator();
		while (iter.hasNext()) {
			String journal = (String) iter.next();
			HashMap docMap = (HashMap) allJournals.get(journal);
			System.out.println(journal);
			System.out.println(docMap.keySet().toString());
		}
	}

	/**
	 * Print a string representation of the Dates occuring for all query results
	 * in this session. For each date print the IDs of the documents with that
	 * date.
	 */
	public void dumpAllDates() {
		Set keys = allDates.keySet();
		Iterator iter = keys.iterator();
		while (iter.hasNext()) {
			String date = (String) iter.next();
			HashMap docMap = (HashMap) allDates.get(date);
			System.out.println(date);
			System.out.println(docMap.keySet().toString());
		}
	}

	/**
	 * Print a string representation of the Authors occuring for all query
	 * results in this session. For each author print the IDs of the documents
	 * with that author.
	 */
	public void dumpAllAuthors() {
		Set keys = allAuthors.keySet();
		Iterator iter = keys.iterator();
		while (iter.hasNext()) {
			String author = (String) iter.next();
			HashMap docMap = (HashMap) allAuthors.get(author);
			System.out.println(author);
			System.out.println(docMap.keySet().toString());
		}
	}

	/**
	 * Print a string representation of the Keywords occuring for all query
	 * results in this session. For each keyword print the IDs of the documents
	 * with that keyword.
	 */
	public void dumpAllKeywords() {
		Set keys = allKeywords.keySet();
		Iterator iter = keys.iterator();
		while (iter.hasNext()) {
			String keyword = (String) iter.next();
			HashMap docMap = (HashMap) allKeywords.get(keyword);
			System.out.println(keyword);
			System.out.println(docMap.keySet().toString());
		}
	}

	/**
	 * Print a string representation of all the result documents returned by
	 * queries in this session.
	 */
	public void dumpAllResults() {
		Set keys = allResults.keySet();
		Iterator iter = keys.iterator();

		while (iter.hasNext()) {
			String docID = (String) iter.next();
			ResultDocument resultDocument = allResults.get(docID);
			System.out.println("____________" + docID + " ___________");
			System.out.println(resultDocument.toString());
		}
	}

	/**
	 * Print all the result documents IDs returned by queries in this session,
	 * along with their titles.
	 */
	public void dumpAllTitles() {
		Set keys = allResults.keySet();
		Iterator iter = keys.iterator();
		while (iter.hasNext()) {
			String docID = (String) iter.next();
			ResultDocument resultDocument = allResults.get(docID);
			System.out.println(docID + "\t" + resultDocument.getTitle());
		}
	}

	/**
	 * Provides the {@link ResultDocument} object for the document with the
	 * given ID
	 * 
	 * @param docID
	 *            is a document identifier, in the form returned by the server
	 *            and available from a {@link QueryOutcome}
	 * @return the {@link ResultDocument} object reflecting the state of the
	 *         result document at the time that this method was called. The
	 *         state can change as more metadata is retrieved for the document
	 *         and the document is returned by further queries.
	 */
	public ResultDocument getDocument(String docID) {
		return allResults.get(docID);
	}

	/**
	 * Implements the actual communication with the server. <b>You can not call
	 * this method directly from your client code.</b>
	 * <p>
	 * Throws an exception and exits if the hosthame is not known or the
	 * connection can't be established.
	 * <p>
	 * 
	 * @param request
	 *            an already well formed string that contains the appropriate
	 *            HTTP headers and a SOAP message (in XML form) that will ask
	 *            the server for some information.
	 * @return a string containing a SOAP message (an XML document) that the
	 *         server returned in response to the request
	 */
	private String doRequest(String request) {
		// System.err.println("Connecting to " + hostname + " on port " + port);
		try {
			try {
				socket = new Socket(hostname, port);
			} catch (SecurityException se) {
				System.err.println("Security exception : " + se);
				System.exit(1);
			}
			toGSDL = new PrintWriter(socket.getOutputStream(), true);
			fromGSDL = new BufferedReader(new InputStreamReader(socket
					.getInputStream()));
		} catch (UnknownHostException e) {
			System.err.println("Don't know about GSDL host: " + hostname);
			System.exit(1);
		} catch (IOException e) {
			System.err.println("IO exception : " + e);
			System.exit(1);
		}

		String result = null;
		toGSDL.println(request);
		// System.err.println("Issued request to " + hostname + " on port " +
		// port);
		try {
			String terminator = "Envelope>";
			String response = "";

			char c;
			do {
				c = (char) fromGSDL.read();
				response = response + c;
			} while (!response.endsWith(terminator));
			toGSDL.close();
			fromGSDL.close();
			socket.close();

			int start = response.indexOf("<?xml");
			result = response.substring(start);
			// System.out.println(result);
			int a = result.indexOf('\n');
			int b = result.indexOf('\n', a + 1);
			while (a != -1 && b != -1) {
				// System.out.println(a + " " +b);
				result = result.substring(0, a - 1) + result.substring(b + 1);
				a = result.indexOf('\n');
				b = result.indexOf('\n', a + 1);
			}
		} catch (IOException e) {
			System.err.println(e);
			System.exit(1);
		}
		return result;
	}

	/**
	 * Produces a SOAP request string, sends it to the server, gets and
	 * processes the response updating the appropriate data structures. Uses the
	 * settings represented in the provided argument to produce a SOAP request
	 * string. The string is sent to the server using the {@link doRequest}
	 * method. The returned XML document is processed and the information
	 * therein is used to store information about the returned documents and
	 * this query.
	 * <p>
	 * This method updates the {@link queryList} and {@link allResults} data
	 * <p>
	 * 
	 * @param query
	 *            a {@link Query} object that must be constructed and passed to
	 *            this method by the calling client application
	 * @return a {@link QueryOutcome} object that stores information about the
	 *         server's response
	 * 
	 */
	public QueryOutcome issueQueryToServer(Query query) {
		QueryOutcome queryOutcome = new QueryOutcome();
		String result = null;
		String requestBody = "<paramList><param name='maxDocs' value='MAXDOCS'/><param name='level' value='Sec'/><param name ='index' value='INDEX'/><param name='matchMode' value='MATCHMODE'/><param name='query' value='QUERY'/><param name='case' value='CASE'/><param name='sortBy' value='SORTBY'/><param name='stem' value='STEM'/><param name='firstDoc' value='FIRSTDOC'/><param name='lastDoc' value='LASTDOC'/></paramList>";
		requestBody = requestBody.replaceFirst("MAXDOCS", query
				.getMaxDocsToReturn());
		requestBody = requestBody.replaceFirst("INDEX", query.getIndex());
		requestBody = requestBody.replaceFirst("MATCHMODE", query
				.getMatchMode());
		requestBody = requestBody.replaceFirst("QUERY", query.getQueryText());
		requestBody = requestBody.replaceFirst("CASE", query.getCasefolding());
		requestBody = requestBody.replaceFirst("SORTBY", query.getSortBy());
		requestBody = requestBody.replaceFirst("STEM", query.getStemming());
		requestBody = requestBody.replaceFirst("FIRSTDOC", query.getFirstDoc());
		requestBody = requestBody.replaceFirst("LASTDOC", query.getLastDoc());
		String request = SOAPrequestMessage.replaceFirst("PROCESSNAME",
				"TextQuery");
		request = request.replaceFirst("PROCESSTYPE", "process");
		request = request.replaceFirst("REQUESTBODY", requestBody);
		request = SOAPrequestHeader.replaceFirst("PROCESSNAME", "TextQuery")
				+ request.length() + "\n\n" + request;

		int firstDoc = java.lang.Integer.parseInt(query.getFirstDoc());

		result = doRequest(request);
		// System.out.println("\n\n" + result + "\n");
		StringReader sr = new StringReader(result);
		InputSource is = new InputSource(sr);

		DOMParser p = new DOMParser();
		try {
			p.parse(is);
		} catch (SAXException se) {
			System.err.println(se);
		} catch (IOException ioe) {
			System.err.println(ioe);
		}
		Document d = p.getDocument();
		NodeList metadataList = d.getElementsByTagName("metadata");
		for (int i = 0; i < metadataList.getLength(); i++) {
			Node n = metadataList.item(i);
			NamedNodeMap nnm = n.getAttributes();
			Node att = nnm.getNamedItem("name");
			if (att.getNodeValue().compareTo("numDocsMatched") == 0) {
				queryOutcome.setHowManyDocsMatched(n.getFirstChild()
						.getNodeValue());
			} else if (att.getNodeValue().compareTo("numDocsReturned") == 0) {
				queryOutcome.setHowManyDocsReturned(n.getFirstChild()
						.getNodeValue());
			}
		}

		NodeList documentList = d.getElementsByTagName("documentNode");
		for (int i = 0; i < documentList.getLength(); i++) {
			Node n = documentList.item(i);
			NamedNodeMap nnm = n.getAttributes();
			Node nid = nnm.getNamedItem("nodeID");
			Node nscore = nnm.getNamedItem("rank");
			String docID = nid.getFirstChild().getNodeValue();
			queryOutcome.addResult(docID, firstDoc + i, nscore.getFirstChild()
					.getNodeValue());
		}
		query.addQueryOutcome(queryOutcome);
		Query q = (Query) query.clone();
		queryList.add(q);

		for (int i = 0; i < documentList.getLength(); i++) {
			Node n = documentList.item(i);
			NamedNodeMap nnm = n.getAttributes();
			Node nid = nnm.getNamedItem("nodeID");
			Node nscore = nnm.getNamedItem("rank");
			String docID = nid.getFirstChild().getNodeValue();

			QueryContext queryContext = new QueryContext(firstDoc + i, nscore
					.getFirstChild().getNodeValue(), q);
			if (allResults.containsKey(docID)) {
				ResultDocument resultDocument = allResults.get(docID);
				resultDocument.incrementFrequencyReturned();
				resultDocument.addQueryContext(queryContext);
				allResults.put(docID, resultDocument);
			} else {
				ResultDocument resultDocument = new ResultDocument();
				resultDocument.addQueryContext(queryContext);
				allResults.put(docID, resultDocument);
			}
		}
		return queryOutcome;
	}

	/**
	 * Produces a SOAP request string, sends it to the server, gets and
	 * processes the response updating the appropriate data structures. Given a
	 * document identifier and the name of a metadata item, this method produces
	 * a SOAP request string. The string is sent to the server using the
	 * {@link doRequest} method.
	 * <p>
	 * The request is simply for the values of the given metadata item of the
	 * given document. <b>If the metadata item for the given document has
	 * already been retrieved from the server, the server is NOT contacted
	 * again.</b>
	 * <p>
	 * The returned XML document is processed. The {@link ResultDocument} object
	 * for the document in question is updated with the returned metadata
	 * information, and the {@link allResults} data is consequently updated.
	 * <p>
	 * If the requested metadata is one of Keywords, Authors, Dates, Journals,
	 * Booktitles then the appropriate data structure is updated.
	 * <p>
	 * The method does not return a value. Private data structures are updated
	 * instead. The calling client application should proceed to access document
	 * metadata using the provided methods.
	 * <p>
	 * 
	 * @param docID
	 *            is a document identifier, in the form returned by the server
	 *            and available from a {@link QueryOutcome}
	 * @param metadata
	 *            is the metadata field whose value is to be retrieved. Valid
	 *            values are
	 *            <ul>
	 *            <li>Title</li>
	 *            <li>Creator (the authors)</li>
	 *            <li>Journal</li>
	 *            <li>Booktitle</li>
	 *            <li>Volume</li>
	 *            <li>Number</li>
	 *            <li>Editor</li>
	 *            <li>Pages</li>
	 *            <li>Publisher</li>
	 *            <li>Date</li>
	 *            <li>Keywords</li>
	 *            <li>Abstract</li>
	 *            </ul>
	 */
	public void getDocumentMetadataFromServer(String docID, String metadata) {
		ResultDocument resultDocument = allResults.get(docID);
		if (resultDocument.metadataExists(metadata)) {
			return;
		}

		String result = null;
		String requestBody = "<paramList><param name='metadata' value='METADATAFIELD'/></paramList><documentNodeList><documentNode nodeID='DOCIDVALUE'/></documentNodeList>";
		requestBody = requestBody.replaceFirst("METADATAFIELD", metadata);
		requestBody = requestBody.replaceFirst("DOCIDVALUE", docID);
		String request = SOAPrequestMessage.replaceFirst("PROCESSNAME",
				"DocumentMetadataRetrieve");
		request = request.replaceFirst("PROCESSTYPE", "process");
		request = request.replaceFirst("REQUESTBODY", requestBody);

		request = SOAPrequestHeader.replaceFirst("PROCESSNAME",
				"DocumentMetadataRetrieve")
				+ request.length() + "\n\n" + request;

		result = doRequest(request);
		StringReader sr = new StringReader(result);
		InputSource is = new InputSource(sr);
		DOMParser p = new DOMParser();
		try {
			p.parse(is);
		} catch (SAXException se) {
			System.err.println(se);
		} catch (IOException ioe) {
			System.err.println(ioe);
		}
		Document d = p.getDocument();
		NodeList metadataList = d.getElementsByTagName("metadata");
		String metadataval = null;
		if (metadataList.getLength() > 0) {
			Node n = metadataList.item(0);
			metadataval = n.getFirstChild().getNodeValue();

			if (metadata.compareTo("Keywords") == 0) {
				String[] keywords = metadataval.split(",");
				for (int i = 0; i < keywords.length; i++) {
					String s = keywords[i].trim().toLowerCase();
					resultDocument.addKeyword(s);
					Set<String> docSet = allKeywords.get(metadataval);
					if (docSet == null) {
						docSet = new HashSet<String>();
					}
					docSet.add(docID);
					allKeywords.put(metadataval, docSet);
				}
			} else if (metadata.compareTo("Creator") == 0) {
				String[] authors = metadataval.split("(,)|( and )");
				// System.err.println(metadataval);
				for (int i = 0; i < authors.length; i++) {
					authors[i] = authors[i].trim().toLowerCase();
				}

				boolean containsExtraName = authors.length % 2 != 0;

				for (int i = 0; i + 1 < authors.length; i = i + 2) {
					String s = authors[i] + ", " + authors[i + 1];

					// Handle names with jr. in them
					if (containsExtraName) {
						if (i + 2 < authors.length
								&& authors[i + 2].contains("jr")) {
							s += " " + authors[i + 2];
							i++;
						}
					}

					s = s.replaceAll("[.]", "");
					// System.err.println(s);
					resultDocument.addAuthor(s);

					Set<String> docSet = allAuthors.get(s);
					if (docSet == null) {
						docSet = new HashSet<String>();
					}
					docSet.add(docID);
					allAuthors.put(s, docSet);
				}
			} else if (metadata.compareTo("Title") == 0) {
				resultDocument.setTitle(metadataval);
			} else if (metadata.compareTo("Booktitle") == 0) {
				resultDocument.setBooktitle(metadataval);
				
				Set<String> docSet = allBooktitles.get(metadataval);
				if (docSet == null) {
					docSet = new HashSet<String>();
				}
				docSet.add(docID);
				allBooktitles.put(metadataval, docSet);
			} else if (metadata.compareTo("Date") == 0) {
				resultDocument.setDate(metadataval.replaceAll("[^0-9]", ""));
				Set<String> docSet = allDates.get(metadataval);
				if (docSet == null) {
					docSet = new HashSet<String>();
				}
				docSet.add(docID);
				allDates.put(metadataval, docSet);
			} else if (metadata.compareTo("Pages") == 0) {
				resultDocument.setPages(metadataval);
			} else if (metadata.compareTo("Journal") == 0) {
				resultDocument.setJournal(metadataval);
				Set<String> docSet = allJournals.get(metadataval);
				if (docSet == null) {
					docSet = new HashSet<String>();
				}
				docSet.add(docID);
				allJournals.put(metadataval, docSet);
			} else if (metadata.compareTo("Volume") == 0) {
				resultDocument.setVolume(metadataval);
			} else if (metadata.compareTo("Number") == 0) {
				resultDocument.setNumber(metadataval);
			} else if (metadata.compareTo("Abstract") == 0) {
				resultDocument.setAbstract(metadataval);
			} else if (metadata.compareTo("Editor") == 0) {
				resultDocument.setEditor(metadataval);
			} else if (metadata.compareTo("Publisher") == 0) {
				resultDocument.setPublisher(metadataval);
			}

		}
		allResults.put(docID, resultDocument);
	}

	public String getClassifierNodeName(String nodeID) {
		String result = null;
		String requestBody = "<paramList><param name='metadata' value='Title'/></paramList><classifierNodeList><classifierNode nodeID='NODEID'/></classifierNodeList>";
		requestBody = requestBody.replaceFirst("NODEID", nodeID);
		String request = SOAPrequestMessage.replaceFirst("PROCESSNAME",
				"ClassifierBrowseMetadataRetrieve");
		request = request.replaceFirst("PROCESSTYPE", "process");
		request = request.replaceFirst("REQUESTBODY", requestBody);

		request = SOAPrequestHeader.replaceFirst("PROCESSNAME",
				"ClassifierBrowseMetadataRetrieve")
				+ request.length() + "\n\n" + request;

		// System.err.println(request);
		result = doRequest(request);
		// System.err.println(result);

		StringReader sr = new StringReader(result);
		InputSource is = new InputSource(sr);
		DOMParser p = new DOMParser();
		try {
			p.parse(is);
		} catch (SAXException se) {
			System.err.println(se);
		} catch (IOException ioe) {
			System.err.println(ioe);
		}

		String returnName = null;

		Document d = p.getDocument();
		// Document d = null;
		NodeList metadataList = d.getElementsByTagName("metadata");
		for (int i = 0; i < metadataList.getLength(); i++) {
			Node n = metadataList.item(i);
			NamedNodeMap nnm = n.getAttributes();
			Node att = nnm.getNamedItem("name");
			if (att.getNodeValue().compareTo("Title") == 0) {
				returnName = n.getFirstChild().getNodeValue();
			}
		}
		return returnName;
	}

	public void getClassifierNodes(String rootNode) {
		String result = null;
		String requestBody = "<paramList><param name='structure' value='children'/></paramList><classifierNodeList><classifierNode nodeID='CLASSIFIER'/></classifierNodeList>";
		requestBody = requestBody.replaceFirst("CLASSIFIER", rootNode);
		String request = SOAPrequestMessage.replaceFirst("PROCESSNAME",
				"ClassifierBrowse");
		request = request.replaceFirst("PROCESSTYPE", "process");
		request = request.replaceFirst("REQUESTBODY", requestBody);

		request = SOAPrequestHeader.replaceFirst("PROCESSNAME",
				"ClassifierBrowse")
				+ request.length() + "\n\n" + request;

		System.err.println(getClassifierNodeName(rootNode));
		// System.err.print(rootNode + "#");

		// System.err.println(request);
		result = doRequest(request);
		// System.err.println(result);

		StringReader sr = new StringReader(result);
		InputSource is = new InputSource(sr);
		DOMParser p = new DOMParser();
		try {
			p.parse(is);
		} catch (SAXException se) {
			System.err.println(se);
		} catch (IOException ioe) {
			System.err.println(ioe);
		}
		Document d = p.getDocument();

		NodeList childList = d.getElementsByTagName("classifierNode");
		NodeList documentList = d.getElementsByTagName("documentNode");
		// System.err.println("\td " + documentList.getLength());
		// System.err.println("\tc " + childList.getLength());

		if (childList.getLength() > 0) {
			for (int i = 0; i < childList.getLength(); i++) {
				Node n = childList.item(i);
				NamedNodeMap nnm = n.getAttributes();
				Node nid = nnm.getNamedItem("nodeID");
				String nodeID = nid.getFirstChild().getNodeValue();

				// System.err.println("\tchild : " + nodeID);

				if (nodeID.compareTo(rootNode) != 0
						&& nodeID.compareTo("2.6.22") != 0) {
					// System.err.println("\t" + nodeID);
					getClassifierNodes(nodeID);
				}
			}
		}
		if (documentList.getLength() > 0)
			System.out.println(getClassifierNodeName(rootNode) + "#"
					+ documentList.getLength());
	}
}
