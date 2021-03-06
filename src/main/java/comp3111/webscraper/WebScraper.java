package comp3111.webscraper;

import java.net.URLEncoder;

import java.util.Collection;
import java.util.Collections; //import Collections class for sorting the items 
import java.util.List;
import java.util.regex.Pattern; // imported Pattern class from Regex for formatting special characters into regular expression

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;// imported exception class for feature 3
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.util.Vector;

/**
 * WebScraper provide a sample code that scrape web content. After it is constructed, you can call the method scrape with a keyword, 
 * the client will go to the default url and parse the page by looking at the HTML DOM.  
 * <br>
 * In this particular sample code, it access to craigslist.org. You can directly search on an entry by typing the URL
 * <br>
 * https://newyork.craigslist.org/search/sss?sort=rel&amp;query=KEYWORD
 *  <br>
 * where KEYWORD is the keyword you want to search.
 * <br>
 * Assume you are working on Chrome, paste the url into your browser and press F12 to load the source code of the HTML. You might be freak
 * out if you have never seen a HTML source code before. Keep calm and move on. Press Ctrl-Shift-C (or CMD-Shift-C if you got a mac) and move your
 * mouse cursor around, different part of the HTML code and the corresponding the HTML objects will be highlighted. Explore your HTML page from
 * body &rarr; section class="page-container" &rarr; form id="searchform" &rarr; div class="content" &rarr; ul class="rows" &rarr; any one of the multiple 
 * li class="result-row" &rarr; p class="result-info". You might see something like this:
 * <br>
 * <pre>
 * {@code
 *    <p class="result-info">
 *        <span class="icon icon-star" role="button" title="save this post in your favorites list">
 *           <span class="screen-reader-text">favorite this post</span>
 *       </span>
 *       <time class="result-date" datetime="2018-06-21 01:58" title="Thu 21 Jun 01:58:44 AM">Jun 21</time>
 *       <a href="https://newyork.craigslist.org/que/clt/d/green-star-polyp-gsp-on-rock/6596253604.html" data-id="6596253604" class="result-title hdrlnk">Green Star Polyp GSP on a rock frag</a>
 *       <span class="result-meta">
 *               <span class="result-price">$15</span>
 *               <span class="result-tags">
 *                   pic
 *                   <span class="maptag" data-pid="6596253604">map</span>
 *               </span>
 *               <span class="banish icon icon-trash" role="button">
 *                   <span class="screen-reader-text">hide this posting</span>
 *               </span>
 *           <span class="unbanish icon icon-trash red" role="button" aria-hidden="true"></span>
 *           <a href="#" class="restore-link">
 *               <span class="restore-narrow-text">restore</span>
 *               <span class="restore-wide-text">restore this posting</span>
 *           </a>
 *       </span>
 *   </p>
 *}
 *</pre>
 * <br>
 * The code 
 * <pre>
 * {@code
 * List<?> items = (List<?>) page.getByXPath("//li[@class='result-row']");
 * }
 * </pre>
 * extracts all result-row and stores the corresponding HTML elements to a list called items. Later in the loop it extracts the anchor tag 
 * &lsaquo; a &rsaquo; to retrieve the display text (by .asText()) and the link (by .getHrefAttribute()). It also extracts  
 * 
 *
 */
public class WebScraper {

	private static final String DEFAULT_URL = "https://newyork.craigslist.org/";
	
	// Feature 2 Preloved will be used as the second selling portal
	private static final String PRELOVED_URL = "https://www.preloved.co.uk/";
	
	// Feature 3 Record # of pages being searched
	private int numPage_craigslist;
	
	// Feature 3 Record # of pages being searched
	private int numPage_preloved;
	
	// Feature 3 Record # of search results
	private int numResults;
	

	//Contains special characters
	private String specialCharacters[] = {
	Pattern.quote("%"), Pattern.quote("/"), Pattern.quote("?"), Pattern.quote("'"), Pattern.quote(";"), Pattern.quote(":"), Pattern.quote("["),
	Pattern.quote("]"), Pattern.quote("{"), Pattern.quote("}"), Pattern.quote("|"), Pattern.quote("\\"), Pattern.quote("`"), Pattern.quote("!"), Pattern.quote("@"), Pattern.quote("#"), Pattern.quote("$"),
	Pattern.quote(","), Pattern.quote("^"), Pattern.quote("&"), Pattern.quote("("), Pattern.quote(")"), Pattern.quote("="), Pattern.quote("+"), Pattern.quote(" ")
	};
	
	//contains corresponding url mapping for special characters
	private String urlCharacters[] = {
	"%25", "%2F", "%3F", "%27", "%3B", "%3A", "%5B", 
	"%5D", "%7B", "%7D","%7C", "%5C", "%60", "%21", "%40", "%23", "%24",
	"%2C", "%5E", "&26", "%28", "%29", "%3D", "%2B", "+"
	};
	
	private WebClient client;

	/**
	 * Default Constructor 
	 */
	public WebScraper() {
		client = new WebClient();
		client.getOptions().setCssEnabled(false);
		client.getOptions().setJavaScriptEnabled(false);
		numPage_craigslist = 0;
		numPage_preloved = 0;
		numResults = 0;
	}

	/**
	 * Scrape web content from two selling portals, Craigslist and Preloved
	 * 
	 * @param keyword The keyword entered by the user
	 * @return A list of Item that has found. A zero size list is return if nothing is found. Null if any exception (e.g. no connectivity)
	 * @exception FailingHttpStatusCodeException Being thrown when the URL doesn't exist. If there is not nay pagination of a particular search, this exception occurs 
	 */
	public List<Item> scrape(String keyword) {
		numPage_craigslist=0;
		numPage_preloved=0;
		numResults = 0;
		try {
			
			//Original source code to scrape one page of items in craigslist
			String formattedKeyword = formatKeyword(keyword);

			String searchUrl = DEFAULT_URL + "search/sss?sort=rel&query=" + URLEncoder.encode(formattedKeyword, "UTF-8");
			HtmlPage page = client.getPage(searchUrl);

			
			List<?> items = (List<?>) page.getByXPath("//li[@class='result-row']");
			
			Vector<Item> result = new Vector<Item>();

			for (int i = 0; i < items.size(); i++) {
				HtmlElement htmlItem = (HtmlElement) items.get(i);
				HtmlAnchor itemAnchor = ((HtmlAnchor) htmlItem.getFirstByXPath(".//p[@class='result-info']/a"));
				HtmlElement spanPrice = ((HtmlElement) htmlItem.getFirstByXPath(".//a/span[@class='result-price']"));

				HtmlElement timeDate = ((HtmlElement) htmlItem.getFirstByXPath(".//time"));

				// It is possible that an item doesn't have any price, we set the price to 0.0
				// in this case
				String itemPrice = spanPrice == null ? "0.0" : spanPrice.asText();

				Item item = new Item();
				item.setTitle(itemAnchor.asText());

				item.setUrl(itemAnchor.getHrefAttribute());
				item.setDate(timeDate.getAttribute("datetime"));
				
				item.setPrice(new Double(itemPrice.replace("$", "")));
				result.add(item);
				numResults++;
			}
			if(items.size() > 0)
				numPage_craigslist++;
			
			//Feature 3 Handle pagination
			try {
				int temp=2;
				int numItems=120;
				String searchUrl_pagination = DEFAULT_URL + "search/sss?s="+ Integer.toString(numItems) +"&sort=rel&query=" + URLEncoder.encode(formattedKeyword, "UTF-8");
				HtmlPage page_crai_pagination = client.getPage(searchUrl_pagination);
				for(int p=0;p<3;p++) {
			//	while(page_crai_pagination!=null) {
					List<?> items_crai_pagination = (List<?>) page_crai_pagination.getByXPath("//li[@class='result-row']");
					for(int i=0;i<items_crai_pagination.size();i++) {
						HtmlElement htmlItem = (HtmlElement) items_crai_pagination.get(i);
						HtmlAnchor itemAnchor = ((HtmlAnchor) htmlItem.getFirstByXPath(".//p[@class='result-info']/a"));
						HtmlElement spanPrice = ((HtmlElement) htmlItem.getFirstByXPath(".//a/span[@class='result-price']"));
						//HtmlElement postdate = ((HtmlElement) htmlItem.getFirstByXPath(".//p[@class='result-info']/time[@class='result-date']"));
						HtmlElement postdate = (HtmlElement) htmlItem.getFirstByXPath(".//time");
						// It is possible that an item doesn't have any price, we set the price to 0.0
						// in this case
						String itemPrice = spanPrice == null ? "0.0" : spanPrice.asText();

						Item item = new Item();
						item.setTitle(itemAnchor.asText());
						item.setUrl(/*DEFAULT_URL + */itemAnchor.getHrefAttribute());
							
						item.setPrice(new Double(itemPrice.replace("$", "")));
						item.setDate(postdate.getAttribute("datetime"));
						result.add(item);
						numResults++;
					}
					numItems = temp*numItems;
					temp++;
					if(items_crai_pagination.size() > 0)
						numPage_craigslist++;
					searchUrl_pagination = DEFAULT_URL + "search/sss?s="+ Integer.toString(numItems) +"&sort=rel&query=" + URLEncoder.encode(formattedKeyword, "UTF-8");
					page_crai_pagination = client.getPage(searchUrl_pagination);
				}
				
			}catch(FailingHttpStatusCodeException e) {} 
			
			//Feature 2 scrape data from Preloved
			String search_Url_Preloved = PRELOVED_URL + "search?keyword=" + URLEncoder.encode(formattedKeyword, "UTF-8");
			HtmlPage page_preloved = client.getPage(search_Url_Preloved);
			List<?> items_preloved = (List<?>) page_preloved.getByXPath("//li[@class='search-result']");
			for (int i=0;i<items_preloved.size();i++) {
				HtmlElement htmlItem = (HtmlElement) items_preloved.get(i);
				HtmlAnchor itemAnchor = ((HtmlAnchor) htmlItem.getFirstByXPath(".//h2/a[@class='search-result__title is-title']"));
				HtmlElement spanPrice = ((HtmlElement) htmlItem.getFirstByXPath(".//span/span[@itemprop='price']"));
				
				// It is possible that an item doesn't have any price, we set the price to 0.0
				// in this case
				String itemPrice = spanPrice == null ? "0.0" : spanPrice.asText();
				
				Item item = new Item();
				item.setTitle(itemAnchor.asText());
				item.setUrl(/*PRELOVED_URL + */itemAnchor.getHrefAttribute());
				
				try {
					//Preloved is an UK selling portal which uses £. 1 GBP = 1.31 USD
					item.setPrice(new Double(itemPrice.replace("£", "").replace(",", "")) * 1.31);
				}
				catch(NumberFormatException e) {
					item.setPrice(0.0);
				}
				//Get post date
				HtmlPage item_page_preloved = client.getPage(itemAnchor.getHrefAttribute());
				HtmlElement postdate = (HtmlElement) item_page_preloved.getFirstByXPath("//li[@class='classified__additional__meta__item classified__timeago']");
				item.setDate(postdate.asText().replace("This advert was updated ",""));
					
				result.add(item);
				numResults++;
			}
			if(items_preloved.size() > 0)
				numPage_preloved++;
			
			//Sort the items
			Collections.sort(result);
			client.close();
			return result;
		} catch (Exception e) {
			System.out.println(e);
		}
		return null;
	}
	
	/**
	 * Check whether the keyword contains special characters and format the keyword if it contains any special character  
	 * 
	 * @param keyword The keyword you want to search
	 * @return A formattedKeyword that will be entered to the url
	 * 
	 **/
	public String formatKeyword(String keyword) {
		String formattedKeyword = keyword;
		for(int i=0;i<specialCharacters.length;i++) {
			formattedKeyword.replaceAll(specialCharacters[i], urlCharacters[i]);
		}
		return formattedKeyword;
	}
	
	/**
	 * Get the number of pages being scraped
	 * 
	 * @param portal The name of the portal
	 * @return The number of pages that are scraped for a particular search
	 */
	public int getNumPage(String portal) {
		if(portal=="craigslist")
			return numPage_craigslist;
		else if(portal=="preloved")
			return numPage_preloved;
		else return 0;
	}
	
	/**
	 * Get the number of items being scraped
	 * 
	 * @return The number of items that are scraped
	 */
	public int getNumResults() {
		return numResults;
	}
}
