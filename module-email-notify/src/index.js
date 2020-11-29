const AWS = require('aws-sdk');

const parameterStore = new AWS.SSM()

module.exports.handler = async(event, context) => {

  console.log('Received event:', JSON.stringify(event));
  
  var topicArn = process.env.TOPIC_ARN;

  var params = {
    Subject: 'STRING_VALUE',
    Message: 'MESSAGE_TEXT',
    TopicArn: topicArn
  };

  // Create promise and SNS service object
  var publishTextPromise = new AWS.SNS({apiVersion: '2010-03-31'}).publish(params).promise();

  // Handle promise's fulfilled/rejected states
  publishTextPromise.then((data) => {
    console.log(`Message ${params.Message} sent to the topic ${params.TopicArn}`);
    console.log("MessageID is " + data.MessageId);
  }).catch((err) => {
    console.error(err, err.stack);
  });
};