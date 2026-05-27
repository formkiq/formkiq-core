process.env.REGION = "us-east-2";
process.env.COGNITO_USER_POOL_ID = "us-east-2_o0TKlWcAb";
process.env.COGNITO_USER_POOL_CLIENT_ID = "5d7uof6ikkki0tj4t39r3mbqg4";

const AWS = require('aws-sdk');
const assert = require('assert');
const lambda = require('../index');

const fs = require('fs').promises;
const path = require('path');

describe('event', () => {

	before(async() => {
	});
	
	it('Connect Expired Token', async() => {
		let text = await readFile('./test/json/connect1.json');
		let response = await lambda.handler(JSON.parse(text), {logStreamName:"test"});
		assert.equal('{"statusCode":400,"body":"unable to verify token"}', JSON.stringify(response));
	});

	it('Connect Missing Token', async() => {
		let text = await readFile('./test/json/connect1.json');
		let response = await lambda.handler(JSON.parse(text), {logStreamName:"test"});
		assert.equal('{"statusCode":400,"body":"unable to verify token"}', JSON.stringify(response));
	});	
});

async function readFile(filePath) {
    return fs.readFile(filePath).then((data) => {
		return data.toString();
    });
}