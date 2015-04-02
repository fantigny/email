package net.anfoya.movie.connector;


/*
 * http://www.rottentomatoes.com/search/json/?catCount=2&q=%s
 *
 * {
 * 		"tv": [
 * 			{"name":"Sean Saves the World"}
 * 		]
 * 		,"movies": [
 * 			{"vanity":"sean_price_sean_p_passion_of_price","name":"Sean Price - Sean P! Passion of Price","subline":"Sean Price","image":"http://resizing.flixster.com/R8qyXCnHOKKYGo3rCMIwgcXd9Hw=/144x146/dkpu1ddg7pbsk.cloudfront.net/movie/11/02/42/11024290_ori.jpg","year":2005}
 * 			,{"vanity":"sean-paul-duttyology","name":"Sean Paul: Duttyology","subline":"Sean Paul","image":"http://resizing.flixster.com/0Gap_g1cZZMZsNCoYrVfQzbHbJA=/180x258/dkpu1ddg7pbsk.cloudfront.net/movie/26/45/264591_ori.jpg","year":2004}
 * 		]
 * 		,"actors": [
 * 			{"vanity":162656350,"name":"Sean Connery","image":"http://resizing.flixster.com/TXU8RXbBesxxOuQy7xmZ8B1WYAg=/280x250/dkpu1ddg7pbsk.cloudfront.net/rtactor/37/64/37641_ori.jpg"}
 * 			,{"vanity":162652874,"name":"Sean Bean","image":"http://d3biamo577v4eu.cloudfront.net/static/images/redesign/actor.default.ori.gif"}
 * 			,{"vanity":162652280,"name":"Sean Penn","image":"http://resizing.flixster.com/SFdfWToJh4m1O-Yra2KBlc3bM0M=/280x250/dkpu1ddg7pbsk.cloudfront.net/rtactor/40/43/40437_ori.jpg"}
 * 			,{"vanity":162659177,"name":"Sean Astin","image":"http://resizing.flixster.com/YFlEETv_Y13AROVHMbKU2QGbQb0=/280x250/dkpu1ddg7pbsk.cloudfront.net/rtactor/83/834_ori.jpg"}
 * 			,{"vanity":162652909,"name":"Seann William Scott","image":"http://resizing.flixster.com/7qPpbmFmMCtGjP5RJxFU3qCzfdc=/280x250/dkpu1ddg7pbsk.cloudfront.net/rtactor/74/7452_ori.jpg"}
 * 			,{"vanity":162707369,"name":"Sean Hayes","image":"http://d3biamo577v4eu.cloudfront.net/static/images/redesign/actor.default.ori.gif"}
 * 			,{"vanity":162660462,"name":"Robert Sean Leonard","image":"http://resizing.flixster.com/ey15Jfw1rFUbET0-94RyBndHHfM=/280x250/dkpu1ddg7pbsk.cloudfront.net/rtactor/43/35/43351_ori.jpg"}
 * 			,{"vanity":162654178,"name":"Sean Patrick Flanery","image":"http://resizing.flixster.com/RDvLkM5tCmr_djvu03dtiZNPreY=/280x250/dkpu1ddg7pbsk.cloudfront.net/rtactor/40/60/40606_ori.jpg"}
 * 			,{"vanity":770689194,"name":"Sean 'P. Diddy' Combs","image":"http://d3biamo577v4eu.cloudfront.net/static/images/redesign/actor.default.ori.gif"}
 * 			,{"vanity":162671441,"name":"Sean Young","image":"http://d3biamo577v4eu.cloudfront.net/static/images/redesign/actor.default.ori.gif"}
 * 			,{"vanity":770672686,"name":"Sean Faris","image":"http://resizing.flixster.com/SmEaNv5sBm9Wfh7quswleLXrb18=/280x250/dkpu1ddg7pbsk.cloudfront.net/rtactor/37/93/37936_ori.jpg"},{"vanity":770755109,"name":"Sean Murray","image":"http://d3biamo577v4eu.cloudfront.net/static/images/redesign/actor.default.ori.gif"}]
 * 		,"tvResults": [
 * 			{"startDate":"Thu Oct 03 00:00:00 PDT 2013","endYear":2013,"vanity":"sean-saves-the-world","startYear":2013,"name":"Sean Saves the World","subline":"Sean Hayes, Linda Lavin, Sami Isler","image":"http://resizing.flixster.com/SSN8UcuBUaJwiuQEJg5L-iZZ0Kg=/180x240/dkpu1ddg7pbsk.cloudfront.net/tv/98/99/98990_ori.jpg"}
 * 		]
 * }
 *
 */

public class RottenTomatoesConnector extends SimpleConnector implements MovieConnector {

	private static final String NAME = "Rotten Tomatoes";
	private static final String HOME_URL = "http://www.rottentomatoes.com";
	private static final String PATTERN_SEARCH = HOME_URL + "/search/?search=%s";

	public RottenTomatoesConnector() {
		super(NAME, HOME_URL, PATTERN_SEARCH);
	}
}
