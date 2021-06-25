const AWS = require('aws-sdk');
const assert = require('assert');
const lambda = require('../index');

const fs = require('fs').promises;
const path = require('path');

AWS.config.update({
  accessKeyId: 'asdjsadkskdskskdk',
  secretAccessKey: 'sdsadsissdiidicdsi',
  region: 'us-east-1',
  endpoint: 'http://localhost:4566'
});

describe('event', () => {

	before(async() => {
		let obj = await new AWS.SNS().createTopic({Name:"test"}).promise();
		process.env.EMAIL_TOPIC_ARN = obj.TopicArn;
		process.env.CONSOLE_URL = "http://localhost:8080/";
	});
	
	it('SQS Event 1', async() => {
		let text = await readFile('./test/json/sqs-event1.json');
		let response = await lambda.handler(JSON.parse(text), {logStreamName:"test"});
		assert.equal("arn:aws:sns:us-east-1:000000000000:test", process.env.EMAIL_TOPIC_ARN);
		assert.equal(1, response.length);
		assert(response[0].MessageId != null);
	});

	it('SQS Event 2 - invalid event', async() => {
		let text = await readFile('./test/json/sqs-event2.json');
		let response = await lambda.handler(JSON.parse(text), {logStreamName:"test"});
		assert.equal("arn:aws:sns:us-east-1:000000000000:test", process.env.EMAIL_TOPIC_ARN);
		assert.equal(0, response.length);
	});
});

async function readFile(filePath) {
    return fs.readFile(filePath).then((data) => {
		return data.toString();
    });
}