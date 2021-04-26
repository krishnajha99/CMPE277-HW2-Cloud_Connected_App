var express = require('express');
var bodyParser = require('body-parser');
var cookieParser = require('cookie-parser');
var session = require('express-session');
var request = require('request');
var app = express();
app.set('view engine', 'ejs');
app.use(express.static(__dirname + '/bower_components'));
app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json());
app.use(cookieParser());

app.get('/', function(req, res) {
    return res.json('API Online');
});

app.post('/webhook', function(req, res) {
	if (!req.body) return res.sendStatus(400);
	res.setHeader('Content-Type', 'application/json');
 	var city = req.body.queryResult.parameters['geo-city'];
	let options = getJSON(city);

	request(options, function(error, response, body) {
		if (!error && response.statusCode == 200) {
			if (body!=null) {
        var obj = JSON.parse('[' + body + ']');
        // return res.json(obj[0].location['name']);

        var responses = city +' şu an  '+ Math.floor(obj[0].current['temp_c']) +' derece ' + obj[0].current.condition['text'];
				let responseObj = {
					'fulfillmentText': ' ',
					'fulfillmentMessages': [{
						'text': {
						    'text': [ responses ]
						}
					}]
				};
				return res.json(responseObj);
			} else {
				let responseObj = {
					'fulfillmentText': ' ',
					'fulfillmentMessages': [{
						'text': {
						    'text': [ city ]
						}
					}]
				};
				return res.json(responseObj);
			}
		} else {
			let responseObj = {
				'fulfillmentText': ' ',
				'fulfillmentMessages': [{
					'text': {
					    'text': [ error ]
					}
				}]
			};
			return res.json(responseObj);
		}
    });
});

function getJSON(link) {
	var json = {
    method: 'GET',
    url: 'http://api.apixu.com/v1/current.json',
    qs:
    {
       key: 'YOUR-KEY',
       q: link,
       lang: 'tr'
     },
     headers:
     {
       'Postman-Token': '051a2f9f-8e41-4b17-bff9-793646e93109',
       'cache-control': 'no-cache',
       'Content-Type': 'application/json' } };

    return json;
}

const PORT = process.env.PORT || 8080;
app.listen(PORT, () => {
  console.log(`App listening on port ${PORT}`);
  console.log('Press Ctrl+C to quit.');
});
